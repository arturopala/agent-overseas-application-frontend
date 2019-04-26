package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers

import org.jsoup.Jsoup
import play.api.test.FakeRequest
import play.api.test.Helpers.redirectLocation
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.{AgentSession, FailureDetails, FileUploadStatus}
import uk.gov.hmrc.agentoverseasapplicationfrontend.stubs.{AgentOverseasApplicationStubs, UpscanStubs}
import uk.gov.hmrc.agentoverseasapplicationfrontend.support.BaseISpec
import uk.gov.hmrc.http.HeaderCarrier

class FileUploadControllerISpec extends BaseISpec with AgentOverseasApplicationStubs with UpscanStubs {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private lazy val controller: FileUploadController = app.injector.instanceOf[FileUploadController]

  private val agentSession = AgentSession()

  "GET /upload/trading-address" should {
    "display the upload trading address form" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession)
      given200UpscanInitiate()

      val result = await(controller.showUploadForm("trading-address")(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 200

      result should containMessages(
        "fileUpload.caption",
        "fileUpload.title.trading-address",
        "fileUpload.p1.trading-address",
        "fileUpload.p2",
        "fileUpload.li.1.trading-address",
        "fileUpload.li.2",
        "fileUpload.li.3",
        "fileUpload.upload",
        "fileUpload.inset",
        "fileUpload.button"
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

  "GET /file-uploaded-successfully/trading-address" should {
    "display the page with correct content" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(tradingAddressUploadStatus = Some(FileUploadStatus("reference","READY",Some("filename")))))

      val result = await(controller.showSuccessfulUploadedForm("trading-address")(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 200

      result should containMessages(
        "fileUpload.success.caption",
        "fileUpload.success.title",
        "fileUpload.success.correctFile.confirmation",
        "fileUpload.success.form.correctFile.yes",
        "fileUpload.success.form.correctFile.no"
      )
    }
  }

  "POST /file-uploaded-successfully/trading-address" should {
    "read the form and redirect to /registered-with-hmrc page if the user selects Yes" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession)

      val request = cleanCredsAgent(FakeRequest().withFormUrlEncodedBody("fileType" -> "trading-address","choice.correctFile" -> "true"))

      val result = await(controller.submitSuccessfulFileUploadedForm(request))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.ApplicationController.showRegisteredWithHmrcForm().url)
    }

    "read the form and redirect to /upload/trading-address page if the user selects No" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession)

      val request = cleanCredsAgent(FakeRequest().withFormUrlEncodedBody("fileType" -> "trading-address", "choice.correctFile" -> "false"))

      val result = await(controller.submitSuccessfulFileUploadedForm(request))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.FileUploadController.showUploadForm("trading-address").url)
    }

    "show the form with errors when invalid value for 'correctFile' is passed in the form" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(tradingAddressUploadStatus = Some(FileUploadStatus("reference","READY",Some("filename")))))

      val request = cleanCredsAgent(FakeRequest().withFormUrlEncodedBody("fileType" -> "trading-address", "choice.correctFile" -> "abcd"))

      val result = await(controller.submitSuccessfulFileUploadedForm(request))

      status(result) shouldBe 200
      result should containMessages(
        "fileUpload.success.caption",
        "fileUpload.success.title",
        "fileUpload.success.correctFile.confirmation",
        "fileUpload.success.form.correctFile.yes",
        "fileUpload.success.form.correctFile.no",
        "error.boolean"
      )
    }

    "show the form with errors when 'correctFile' field is missing the form" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(tradingAddressUploadStatus = Some(FileUploadStatus("reference","READY",Some("filename")))))

      val request = cleanCredsAgent(FakeRequest().withFormUrlEncodedBody("fileType" -> "trading-address", "choice.correctabxgd" -> "true"))

      val result = await(controller.submitSuccessfulFileUploadedForm(request))

      status(result) shouldBe 200
      result should containMessages(
        "fileUpload.success.caption",
        "fileUpload.success.title",
        "fileUpload.success.correctFile.confirmation",
        "fileUpload.success.form.correctFile.yes",
        "fileUpload.success.form.correctFile.no",
        "fileUpload.correctFile.no-radio.selected"
      )
    }

    "show the form with errors when 'fileType' field has been modified by the user and contains invalid value" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(tradingAddressUploadStatus = Some(FileUploadStatus("reference","READY",Some("filename")))))

      val request = cleanCredsAgent(FakeRequest().withFormUrlEncodedBody("fileType" -> "invalid", "choice.correctFile" -> "true"))

      an[RuntimeException] shouldBe thrownBy(await(controller.submitSuccessfulFileUploadedForm(request)))
    }
  }

  "GET /file-upload-failed" should {

    val failureDetails = FailureDetails("QUARANTINED","a virus was found!")
    val fileUploadStatus = FileUploadStatus("reference","READY",Some("filename"),Some(failureDetails))
    val tradingAddressAddressUploadStatus = FileUploadStatus("reference","READY",Some("filename"))

    "display page as expected" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(tradingAddressUploadStatus = Some(tradingAddressAddressUploadStatus)))

      val request = cleanCredsAgent(FakeRequest())

      val result = await(controller.showUploadFailedPage("trading-address")(request))

      status(result) shouldBe 200

      result should containMessages(
        "fileUpload.failed.caption",
        "fileUpload.failed.title",
        "fileUpload.failed.p1",
        "fileUpload.failed.try-again.label"
      )

      val tradingAddressUploadFormUrl = routes.FileUploadController.showUploadForm("trading-address").url

      val doc = Jsoup.parse(bodyOf(result))
      val tryAgainLink = doc.getElementById("file-upload-failed")
      tryAgainLink.text() shouldBe "Try again"
      tryAgainLink.attr("href") shouldBe tradingAddressUploadFormUrl

      result should containLink("button.back", tradingAddressUploadFormUrl)
    }
  }

  "GET /upload/amls" should {
    "display the upload amls form" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession)
      given200UpscanInitiate()

      val result = await(controller.showUploadForm("amls")(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 200

      result should containMessages(
        "fileUpload.caption",
        "fileUpload.title.amls",
        "fileUpload.p1.amls",
        "fileUpload.p2",
        "fileUpload.li.1.amls",
        "fileUpload.li.2",
        "fileUpload.li.3",
        "fileUpload.upload",
        "fileUpload.inset",
        "fileUpload.button"
      )
    }
  }

  "POST /file-uploaded-successfully/amls" should {
    "read the form and redirect to /contact-details page if the user selects Yes" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession)

      val request = cleanCredsAgent(FakeRequest().withFormUrlEncodedBody("fileType" -> "amls", "choice.correctFile" -> "true"))

      val result = await(controller.submitSuccessfulFileUploadedForm(request))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.ApplicationController.showContactDetailsForm().url)
    }

    "read the form and redirect to /upload/amls page if the user selects No" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession)

      val request = cleanCredsAgent(FakeRequest().withFormUrlEncodedBody("fileType" -> "amls", "choice.correctFile" -> "false"))

      val result = await(controller.submitSuccessfulFileUploadedForm(request))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.FileUploadController.showUploadForm("amls").url)
    }
  }

  "GET /upload/trn" should {
    "display the upload trn form" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession)
      given200UpscanInitiate()

      val result = await(controller.showUploadForm("trn")(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 200

      result should containMessages(
        "fileUpload.caption",
        "fileUpload.title.trn",
        "fileUpload.p1.trn",
        "fileUpload.p2",
        "fileUpload.li.1.trn",
        "fileUpload.li.2",
        "fileUpload.li.3",
        "fileUpload.upload",
        "fileUpload.inset",
        "fileUpload.button"
      )
    }
  }

  "POST /file-uploaded-successfully/trn" should {
    "read the form and redirect to /check-your-answers page if the user selects Yes" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession)

      val request = cleanCredsAgent(FakeRequest().withFormUrlEncodedBody("fileType" -> "trn","choice.correctFile" -> "true"))

      val result = await(controller.submitSuccessfulFileUploadedForm(request))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.ApplicationController.showCheckYourAnswers().url)
    }

    "read the form and redirect to /upload/trn page if the user selects No" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession)

      val request = cleanCredsAgent(FakeRequest().withFormUrlEncodedBody("fileType" -> "trn", "choice.correctFile" -> "false"))

      val result = await(controller.submitSuccessfulFileUploadedForm(request))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.FileUploadController.showUploadForm("trn").url)
    }
  }
}
