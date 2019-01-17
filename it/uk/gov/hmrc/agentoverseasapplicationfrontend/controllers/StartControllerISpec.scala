package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers

import play.api.test.FakeRequest
import uk.gov.hmrc.agentoverseasapplicationfrontend.support.BaseISpec
import uk.gov.hmrc.agentoverseasapplicationfrontend.stubs._
import play.api.test.Helpers._
import play.twirl.api.Html

class StartControllerISpec extends BaseISpec with AgentOverseasApplicationStubs {

  private lazy val controller = app.injector.instanceOf[StartController]

  "GET /not-agent" should {
    "display the non-agent  page when the current user is logged in" in {

      val result = await(controller.showNotAgent(basicRequest(FakeRequest())))

      status(result) shouldBe 200
    }
  }

  "GET / " should {
    "display the status_rejected page if all of the applications returned from BE are rejected status" in {
      given200GetOverseasApplications(true)
      val result = await(controller.root(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 200

      result should containMessages("statusRejected.title",
        "statusRejected.heading","statusRejected.para3", "statusRejected.link.text")

      result should containSubstrings(htmlEscapedMessage("statusRejected.para1","Tradingname"))

      bodyOf(result).contains(
        htmlEscapedMessage("statusRejected.link", routes.ApplicationController.showAntiMoneyLaunderingForm().url),
        htmlEscapedMessage("statusRejected.para2", "email@domain.com"))
    }

    "redirect to anti-money-laundering page if ANY of the applications returned from the BE are NOT rejected" in {
      given200GetOverseasApplications(false)
      val result = await(controller.root(cleanCredsAgent(FakeRequest())))

      redirectLocation(result).get shouldBe routes.ApplicationController.showAntiMoneyLaunderingForm().url
    }
  }
}