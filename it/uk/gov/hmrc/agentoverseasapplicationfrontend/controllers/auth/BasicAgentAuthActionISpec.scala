package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.auth

import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers.redirectLocation
import uk.gov.hmrc.agentoverseasapplicationfrontend.support.BaseISpec
import uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.routes
import scala.language.postfixOps

class BasicAgentAuthActionISpec extends BaseISpec {

  implicit val timeout = akka.util.Timeout {
    import scala.concurrent.duration._
    5 seconds
  }

  val authAction = app.injector.instanceOf[BasicAgentAuthAction]

  class TestController(authAction: BasicAgentAuthAction) {

    def withValidApplicant = authAction { implicit request => Ok }
  }

  "BasicAuthAction" should {
    val testController = new TestController(authAction)

    "allow logged in user" in {
      val result = await(testController.withValidApplicant(basicAgentRequest(FakeRequest())))

      status(result) shouldBe 200
    }

    "303 showNotAgent, user without affinityGroup: Agent is not allowed for this service" in {
      givenUnauthorisedWith("UnsupportedAffinityGroup")

      val result = await(testController.withValidApplicant(FakeRequest()))

      status(result) shouldBe 303
      redirectLocation(result).get shouldBe routes.StartController.showNotAgent().url
    }

    "redirect to gg sign in page if the user is not logged in" in {
      givenUnauthorisedWith("MissingBearerToken")
      val result = await(testController.withValidApplicant(FakeRequest()))

      status(result) shouldBe  303

      redirectLocation(result).get shouldBe "/gg/sign-in?continue=%2F&origin=agent-overseas-application-frontend"
    }
  }
}

