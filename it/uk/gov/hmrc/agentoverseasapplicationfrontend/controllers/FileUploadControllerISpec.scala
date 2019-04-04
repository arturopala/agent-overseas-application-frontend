package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers

import play.api.test.FakeRequest
import play.api.test.Helpers.redirectLocation
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.AgentSession
import uk.gov.hmrc.agentoverseasapplicationfrontend.stubs.AgentOverseasApplicationStubs
import uk.gov.hmrc.agentoverseasapplicationfrontend.support.BaseISpec
import uk.gov.hmrc.http.HeaderCarrier

class FileUploadControllerISpec extends BaseISpec with AgentOverseasApplicationStubs {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private lazy val controller: FileUploadController = app.injector.instanceOf[FileUploadController]

  private val agentSession = AgentSession()


  "GET /trading-address-upload" should {
    "display the upload trading address form" in {
      sessionStoreService.currentSession.agentSession =
        Some(agentSession)

      val result = await(controller.showTradingAddressUploadForm(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 200

      result should containMessages(
        "fileUploadTradingAddress.caption",
        "fileUploadTradingAddress.title",
        "fileUploadTradingAddress.p1",
        "fileUploadTradingAddress.p2",
        "fileUploadTradingAddress.li.1",
        "fileUploadTradingAddress.li.2",
        "fileUploadTradingAddress.li.3",
        "fileUploadTradingAddress.upload",
        "fileUploadTradingAddress.inset",
        "fileUploadTradingAddress.button"
      )

    }
  }

  "POST /trading-address-upload" should {
    "redirect to / GET registered-with-hmrc" in {
      sessionStoreService.currentSession.agentSession =
        Some(agentSession)

      val result = await(controller.submitTradingAddressUploadForm(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 303

      redirectLocation(result) shouldBe Some("/agent-services/apply-from-outside-uk/registered-with-hmrc")
    }
  }

  "GET /trading-address-no-js-check-file" should {
    "display the page with correct content" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession)

      val result = await(controller.showTradingAddressNoJsCheckPage(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 200

      result should containMessages(
        "fileUploadTradingAddress.no_js_page.caption",
        "fileUploadTradingAddress.no_js_page.title",
        "fileUploadTradingAddress.no_js_page.p1",
        "fileUploadTradingAddress.no_js_page.p2"
      )
    }
  }
}
