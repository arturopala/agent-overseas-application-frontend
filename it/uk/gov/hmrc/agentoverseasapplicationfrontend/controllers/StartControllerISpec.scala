package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers

import play.api.test.FakeRequest
import uk.gov.hmrc.agentoverseasapplicationfrontend.support.BaseISpec

class StartControllerISpec extends BaseISpec {

  private lazy val controller = app.injector.instanceOf[StartController]

  "GET /not-agent" should {
    "display the non-agent  page when the current user is logged in" in {

      val result = await(controller.showNotAgent(basicRequest(FakeRequest())))

      status(result) shouldBe 200
    }
  }
}