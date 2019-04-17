package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers

import org.jsoup.Jsoup
import play.api.Application
import play.api.test.FakeRequest
import play.api.test.Helpers.redirectLocation
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.{AgentSession, FailureDetails, FileUploadStatus}
import uk.gov.hmrc.agentoverseasapplicationfrontend.stubs.{AgentOverseasApplicationStubs, UpscanStubs}
import uk.gov.hmrc.agentoverseasapplicationfrontend.support.BaseISpec
import uk.gov.hmrc.http.HeaderCarrier
import play.filters.csrf.CSRF.Token
import play.filters.csrf.{CSRFConfigProvider, CSRFFilter}

class FileUploadControllerISpec extends BaseISpec with AgentOverseasApplicationStubs with UpscanStubs {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private lazy val controller: FileUploadController = app.injector.instanceOf[FileUploadController]

  private val agentSession = AgentSession()

  "GET /trading-address-upload" should {
    "display the upload trading address form" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession)
      given200UpscanInitiate()

      val result = await(controller.showTradingAddressUploadForm(cleanCredsAgent(addToken(FakeRequest()))))

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
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(tradingAddressUploadStatus = Some(FileUploadStatus("reference","READY",Some("filename")))))

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
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(tradingAddressUploadStatus = Some(FileUploadStatus("reference","READY",Some("filename")))))

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
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(tradingAddressUploadStatus = Some(FileUploadStatus("reference","READY",Some("filename")))))

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

    val failureDetails = FailureDetails("QUARANTINED","a virus was found!")
    val fileUploadStatus = FileUploadStatus("reference","READY",Some("filename"),Some(failureDetails))
    val tradingAddressAddressUploadStatus = FileUploadStatus("reference","READY",Some("filename"))

    "display page as expected" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(tradingAddressUploadStatus = Some(tradingAddressAddressUploadStatus)))

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

  private  def addToken[T](fakeRequest: FakeRequest[T])(implicit app: Application) = {
    val csrfConfig     = app.injector.instanceOf[CSRFConfigProvider].get
    val csrfFilter     = app.injector.instanceOf[CSRFFilter]
    val token          = csrfFilter.tokenProvider.generateToken

    fakeRequest.copyFakeRequest(tags = fakeRequest.tags ++ Map(
      Token.NameRequestTag  -> csrfConfig.tokenName,
      Token.RequestTag      -> token
    )).withHeaders((csrfConfig.headerName, token))
  }
}
