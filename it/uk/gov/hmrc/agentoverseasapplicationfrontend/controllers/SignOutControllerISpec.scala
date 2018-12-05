package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers

import play.api.test.FakeRequest
import play.api.test.Helpers.redirectLocation
import uk.gov.hmrc.agentoverseasapplicationfrontend.support.BaseISpec

class SignOutControllerISpec extends BaseISpec{

  implicit val timeout = akka.util.Timeout {
    import scala.concurrent.duration._
    5 seconds
  }

  private lazy val controller: SignOutController = app.injector.instanceOf[SignOutController]

  "signOut" should {
    "303 lose existing session and redirect to gg sign-in" in {
      val someExistingKey = "storedInSessionKey"
      implicit val requestWithSession = FakeRequest().withSession(someExistingKey -> "valueForKeyInSession")

      val result = await(controller.signOut(requestWithSession))

      result.session.get(someExistingKey) shouldBe None
      redirectLocation(result) shouldBe Some("/baseISpec/gg/sign-in")
    }
  }
}
