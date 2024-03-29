package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers

import play.api.test.FakeRequest
import play.api.test.Helpers.{LOCATION, redirectLocation}
import uk.gov.hmrc.agentoverseasapplicationfrontend.support.BaseISpec
import scala.language.postfixOps

class SignOutControllerISpec extends BaseISpec{

  private val controller: SignOutController = app.injector.instanceOf[SignOutController]

  "signOut" should {
    "303 lose existing session and redirect to gg sign-in" in {
      val someExistingKey = "storedInSessionKey"
      implicit val requestWithSession = FakeRequest().withSession(someExistingKey -> "valueForKeyInSession")

      val result = await(controller.signOut(requestWithSession))

      result.session.get(someExistingKey) shouldBe None
      redirectLocation(result) shouldBe Some("/baseISpec/gg/sign-in")
    }
  }

  "signOutWithContinueUrl" should {
    "303 to GG-registration-frontend with continueUrl to start of overseas journey" in {
      val someExistingKey = "storedInSessionKey"

      implicit val request = FakeRequest().withSession(someExistingKey -> "testValue")
      val result = await(controller.signOutWithContinueUrl(request))

      result.session.get(someExistingKey) shouldBe None
      redirectLocation(result).get shouldBe "http://localhost:8571/government-gateway-registration-frontend?accountType=agent&origin=unknown&continue=%2Fagent-services%2Fapply-from-outside-uk%2Fmoney-laundering"
    }
  }

  "startSurvey" should {
    "redirect to feedback survey page" in {
      val result = await(controller.startFeedbackSurvey(basicRequest(FakeRequest())))

      status(result) shouldBe 303
      result.header.headers(LOCATION) should include("/feedback/OVERSEAS_AGENTS")
    }
  }
}
