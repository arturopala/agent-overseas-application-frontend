package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers

import org.jsoup.Jsoup
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
      sessionStoreService.currentSession.agentSession = Some(agentSession)

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
      sessionStoreService.currentSession.agentSession = Some(agentSession)

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

  "GET /file-uploaded-successfully" should {
    "display the page with correct content" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession)

      val result = await(controller.showSuccessfulFileUploadedForm(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 200

      result should containMessages(
        "fileUploadTradingAddress.success.caption",
        "fileUploadTradingAddress.success.title",
        "fileUploadTradingAddress.success.correctFile.confirmation",
        "fileUploadTradingAddress.success.form.correctFile.yes",
        "fileUploadTradingAddress.success.form.correctFile.no"
      )
    }
  }

  "POST /file-uploaded-successfully" should {
    "read the form and redirect to /registered-with-hmrc page if the user selects Yes" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession)

      val request = cleanCredsAgent(FakeRequest().withFormUrlEncodedBody("correctFile" -> "true"))

      val result = await(controller.submitSuccessfulFileUploadedForm(request))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.ApplicationController.showRegisteredWithHmrcForm().url)
    }

    "read the form and redirect to /trading-address-upload page if the user selects No" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession)

      val request = cleanCredsAgent(FakeRequest().withFormUrlEncodedBody("correctFile" -> "false"))

      val result = await(controller.submitSuccessfulFileUploadedForm(request))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.FileUploadController.showTradingAddressUploadForm().url)
    }

    "show the form with errors when invalid value for 'correctFile' is passed in the form" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession)

      val request = cleanCredsAgent(FakeRequest().withFormUrlEncodedBody("correctFile" -> "abcd"))

      val result = await(controller.submitSuccessfulFileUploadedForm(request))

      status(result) shouldBe 200
      result should containMessages(
        "fileUploadTradingAddress.success.caption",
        "fileUploadTradingAddress.success.title",
        "fileUploadTradingAddress.success.correctFile.confirmation",
        "fileUploadTradingAddress.success.form.correctFile.yes",
        "fileUploadTradingAddress.success.form.correctFile.no",
        "error.boolean"
      )
    }

    "show the form with errors when 'correctFile' field is missing the form" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession)

      val request = cleanCredsAgent(FakeRequest().withFormUrlEncodedBody("correctabxgd" -> "true"))

      val result = await(controller.submitSuccessfulFileUploadedForm(request))

      status(result) shouldBe 200
      result should containMessages(
        "fileUploadTradingAddress.success.caption",
        "fileUploadTradingAddress.success.title",
        "fileUploadTradingAddress.success.correctFile.confirmation",
        "fileUploadTradingAddress.success.form.correctFile.yes",
        "fileUploadTradingAddress.success.form.correctFile.no",
        "fileUploadTradingAddress.correctFile.no-radio.selected"
      )
    }
  }

  "GET /file-upload-failed" should {
    "display page as expected" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession)

      val request = cleanCredsAgent(FakeRequest())

      val result = await(controller.showUploadFailedPage(request))

      status(result) shouldBe 200

      result should containMessages(
        "fileUploadTradingAddress.failed.caption",
        "fileUploadTradingAddress.failed.title",
        "fileUploadTradingAddress.failed.p1",
        "fileUploadTradingAddress.failed.try-again.label"
      )

      val tradingAddressUploadFormUrl = routes.FileUploadController.showTradingAddressUploadForm().url

      val doc = Jsoup.parse(bodyOf(result))
      val tryAgainLink = doc.getElementById("file-upload-failed")
      tryAgainLink.text() shouldBe "Try again"
      tryAgainLink.attr("href") shouldBe tradingAddressUploadFormUrl

      result should containLink("button.back", tradingAddressUploadFormUrl)
    }
  }
}
