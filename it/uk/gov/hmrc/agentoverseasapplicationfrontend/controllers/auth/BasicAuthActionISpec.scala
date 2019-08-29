package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.auth

import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers.redirectLocation
import uk.gov.hmrc.agentoverseasapplicationfrontend.support.BaseISpec
import scala.language.postfixOps

class BasicAuthActionISpec extends BaseISpec {

  implicit val timeout = akka.util.Timeout {
    import scala.concurrent.duration._
    5 seconds
  }

  val authAction = app.injector.instanceOf[BasicAuthAction]

  class TestController(authAction: BasicAuthAction) {

    def withValidApplicant = authAction { implicit request => Ok }
  }

  "BasicAuthAction" should {
    val testController = new TestController(authAction)

    "allow logged in user" in {
      val result = await(testController.withValidApplicant(basicRequest(FakeRequest())))

      status(result) shouldBe 200
    }

    "redirect to gg sign in page if the user is not logged in" in {
      givenUnauthorisedWith("MissingBearerToken")
      val result = await(testController.withValidApplicant(FakeRequest()))

      status(result) shouldBe  303

      redirectLocation(result).get shouldBe "/gg/sign-in?continue=%2F&origin=agent-overseas-application-frontend"
    }
  }
}
