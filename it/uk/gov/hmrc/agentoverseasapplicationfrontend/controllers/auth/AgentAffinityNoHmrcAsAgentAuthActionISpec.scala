package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.auth

import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers.redirectLocation
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.AgentSession
import uk.gov.hmrc.agentoverseasapplicationfrontend.stubs.AgentOverseasApplicationStubs
import uk.gov.hmrc.agentoverseasapplicationfrontend.support.BaseISpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.routes
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.ApplicationStatus.{Accepted, AttemptingRegistration, Complete, Registered}
import uk.gov.hmrc.auth.core.UnsupportedCredentialRole

import scala.language.postfixOps

class AgentAffinityNoHmrcAsAgentAuthActionISpec extends BaseISpec with AgentOverseasApplicationStubs {

  implicit val timeout = akka.util.Timeout {
    import scala.concurrent.duration._
    5 seconds
  }
  implicit val hc = HeaderCarrier()
  val authAction = app.injector.instanceOf[AgentAffinityNoHmrcAsAgentAuthAction]

  class TestController(authAction: AgentAffinityNoHmrcAsAgentAuthAction) {

    def withValidApplicant = authAction { implicit validApplicantRequest => Ok(validApplicantRequest.authProviderId) }
  }

  "AgentAffinityNoHmrcAsAgentAuthAction" should {
    val testController = new TestController(authAction)

    "ALLOW logged in user with affinityGroup Agent" in {
      initialiseAgentSession
      val result = await(testController.withValidApplicant(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 200
    }

    "FORBIDDEN when has HMRC-AS-AGENT enrolment" in {
      val authenticatedRequest = agentWithAuthorisedEnrolment(FakeRequest(), Enrolment("HMRC-AS-AGENT", "AgentReferenceNumber", "TARN0000001"))
      val result = await(testController.withValidApplicant(authenticatedRequest))

      status(result) shouldBe 303
      redirectLocation(result).get shouldBe "http://localhost:9401/agent-services-account"
    }

    "redirect to gg sign in page if the user is not logged in" in {
      givenUnauthorisedWith("MissingBearerToken")
      val result = await(testController.withValidApplicant(FakeRequest()))

      status(result) shouldBe 303

      redirectLocation(result).get shouldBe "/gg/sign-in?continue=%2F&origin=agent-overseas-application-frontend"
    }

    "throw an exception when the user has no credentials" in {
      initialiseAgentSession
      val authenticatedRequest = agentWithNoEnrolmentsOrCreds(FakeRequest())
      intercept[UnsupportedCredentialRole] {
        await(testController.withValidApplicant(authenticatedRequest))
      }.getMessage shouldBe "User has no credentials"
    }

    "given UnsupportedAffinityGroup" in {
      givenUnauthorisedWith("UnsupportedAffinityGroup")

      val result = await(testController.withValidApplicant(FakeRequest()))
      status(result) shouldBe 303
      redirectLocation(result).get shouldBe "/agent-services/apply-from-outside-uk/not-agent"
    }

    "skip routing checks when agentSession has been initialised by journey" in {
      initialiseAgentSession

      val result = await(testController.withValidApplicant(cleanCredsAgent(FakeRequest())))
      status(result) shouldBe 200
    }

    "check for existing application when no AgentSession defined/initialised by journey" when {
      "no existing application found for userId then show AMLS page" in {
        given404OverseasApplications()
        val result = await(testController.withValidApplicant(cleanCredsAgent(FakeRequest())))

        redirectLocation(result).get shouldBe routes.AntiMoneyLaunderingController.showAntiMoneyLaunderingForm().url
        isAgentSessionInitialised shouldBe true
      }

      "application found with status PENDING, show appNotReady page" in {
        given200OverseasPendingApplication()
        val result = await(testController.withValidApplicant(cleanCredsAgent(FakeRequest())))

        redirectLocation(result).get shouldBe routes.StartController.applicationStatus().url
        isAgentSessionInitialised shouldBe false
      }

      "application found with status REJECTED, show appRejected page and initialise AgentSession" in {
        given200GetOverseasApplications(true)

        val result = await(testController.withValidApplicant(cleanCredsAgent(FakeRequest())))
        redirectLocation(result).get shouldBe routes.StartController.applicationStatus().url
        isAgentSessionInitialised shouldBe true
      }

      Seq(Accepted, AttemptingRegistration, Registered, Complete).foreach(status =>
        s"application found with status: ${status.key}, Redirect to overseas-subscription-frontend" in {
          given200OverseasRedirectStatusApplication(status.key)

          val result = await(testController.withValidApplicant(cleanCredsAgent(FakeRequest())))
          redirectLocation(result).get shouldBe "http://localhost:9403/agent-services/apply-from-outside-uk/create-account"
          isAgentSessionInitialised shouldBe false
        }
      )
    }
  }

  private def isAgentSessionInitialised(implicit hc: HeaderCarrier): Boolean = {
    await(sessionStoreService.fetchAgentSession).isDefined
  }

  private def initialiseAgentSession(implicit hc: HeaderCarrier): Unit = {
    await(sessionStoreService.cacheAgentSession(AgentSession()))
  }
}