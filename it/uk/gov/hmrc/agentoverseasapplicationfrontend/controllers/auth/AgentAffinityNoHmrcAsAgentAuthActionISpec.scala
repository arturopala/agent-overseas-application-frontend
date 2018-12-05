package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.auth

import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers.redirectLocation
import uk.gov.hmrc.agentoverseasapplicationfrontend.support.BaseISpec

class AgentAffinityNoHmrcAsAgentAuthActionISpec extends BaseISpec {

  implicit val timeout = akka.util.Timeout {
    import scala.concurrent.duration._
    5 seconds
  }

  val authAction = app.injector.instanceOf[AgentAffinityNoHmrcAsAgentAuthAction]

  class TestController(authAction: AgentAffinityNoHmrcAsAgentAuthAction) {

    def withValidApplicant = authAction { implicit validApplicantRequest => Ok(validApplicantRequest.authProviderId) }
  }

  "AgentAffinityNoHmrcAsAgentAuthAction" should {
    val testController = new TestController(authAction)

    "ALLOW logged in user with affinityGroup Agent" in {
      val result = await(testController.withValidApplicant(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 200
    }

    "FORBIDDEN when has HMRC-AS-AGENT enrolment" in {
      val authenticatedRequest = agentWithAuthorisedEnrolment(FakeRequest(), Enrolment("HMRC-AS-AGENT", "AgentReferenceNumber", "TARN0000001"))
      val result = await(testController.withValidApplicant(authenticatedRequest))

      status(result) shouldBe 403
    }

    "redirect to gg sign in page if the user is not logged in" in {
      givenUnauthorisedWith("MissingBearerToken")
      val result = await(testController.withValidApplicant(FakeRequest()))

      status(result) shouldBe  303

      redirectLocation(result).get shouldBe "/gg/sign-in?continue=%2F&origin=agent-overseas-application-frontend"
    }

    "given UnsupportedAffinityGroup" in {
      givenUnauthorisedWith("UnsupportedAffinityGroup")

      val result = await(testController.withValidApplicant(FakeRequest()))
      status(result) shouldBe 303
      redirectLocation(result).get shouldBe "/agent-services/apply-from-outside-uk/not-agent"
    }
  }
}