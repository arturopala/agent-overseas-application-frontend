package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers

import play.api.test.FakeRequest
import play.api.test.Helpers.{LOCATION, redirectLocation}
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.PersonalDetailsChoice.RadioOption
import uk.gov.hmrc.agentoverseasapplicationfrontend.models._
import uk.gov.hmrc.agentoverseasapplicationfrontend.stubs.AgentOverseasApplicationStubs
import uk.gov.hmrc.agentoverseasapplicationfrontend.support.BaseISpec
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier

class TradingAddressControllerISpec extends BaseISpec with AgentOverseasApplicationStubs {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val contactDetails = ContactDetails("test", "last", "senior agent", "12345", "test@email.com")
  private val overseasAddress = OverseasAddress("line 1", "line 2", None, None, countryCode = "IE")
  private val amlsDetails = AmlsDetails("Keogh Chartered Accountants", Some("123456"))
  private val personalDetails = PersonalDetailsChoice(Some(RadioOption.NinoChoice), Some(Nino("AB123456A")), None)

  private val fileUploadStatus: Option[FileUploadStatus] = Some(FileUploadStatus(reference = "ref", fileStatus = "status", fileName = Some("fileName")))

  private val agentSession = AgentSession(
    amlsDetails = Some(amlsDetails),
    contactDetails = Some(contactDetails),
    tradingName = Some("some name"),
    overseasAddress = Some(overseasAddress),
    personalDetails = Some(personalDetails)
  )

  private lazy val controller: TradingAddressController = app.injector.instanceOf[TradingAddressController]

  "GET /main-business-address" should {
    "display the trading address form" in {
      sessionStoreService.currentSession.agentSession =
        Some(agentSession.copy(overseasAddress = None, changingAnswers = true))

      val result = await(controller.showMainBusinessAddressForm(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 200

      result should containMessages(
        "mainBusinessAddress.caption",
        "mainBusinessAddress.title"
      )
      result should containSubstrings(routes.ApplicationController.showCheckYourAnswers().url)
    }

    "redirect to /money-laundering-registration when session not found" in {

      val authenticatedRequest = cleanCredsAgent(FakeRequest())

      val result = await(controller.showMainBusinessAddressForm(authenticatedRequest))

      status(result) shouldBe 303

      redirectLocation(result) shouldBe Some(routes.AntiMoneyLaunderingController.showMoneyLaunderingRequired().url)
    }
  }

  "POST /main-business-address" should {
    "submit form and then redirect to trading-address-upload page" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(overseasAddress = None))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("addressLine1" -> "line1", "addressLine2" -> "line2", "countryCode" -> "IE")

      val result = await(controller.submitMainBusinessAddress(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.FileUploadController.showTradingAddressUploadForm().url

      val tradingAddress = await(sessionStoreService.fetchAgentSession).get.overseasAddress

      tradingAddress shouldBe Some(OverseasAddress("line1", "line2", None, None, "IE"))
    }

    "submit form and then redirect to check-your-answers page if user is changing answers" in {
      sessionStoreService.currentSession.agentSession =
        Some(agentSession.copy(overseasAddress = None, changingAnswers = true))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("addressLine1" -> "line1", "addressLine2" -> "line2", "countryCode" -> "IE")

      val result = await(controller.submitMainBusinessAddress(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showCheckYourAnswers().url

      val session = await(sessionStoreService.fetchAgentSession).get

      session.overseasAddress shouldBe Some(OverseasAddress("line1", "line2", None, None, "IE"))

      //should revert to normal state after amending is successful
      session.changingAnswers shouldBe false
    }

    "show validation errors when form data is incorrect" when {
      "address line 1 is blank" in {
        sessionStoreService.currentSession.agentSession =
          Some(agentSession.copy(overseasAddress = None, changingAnswers = true))

        implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
          .withFormUrlEncodedBody("addressLine1" -> "", "addressLine2" -> "line2", "countryCode" -> "IE")

        val result = await(controller.submitMainBusinessAddress(authenticatedRequest))

        status(result) shouldBe 200

        result should containMessages("error.addressline.1.blank")
      }
      "country code is GB" in {
        sessionStoreService.currentSession.agentSession =
          Some(agentSession.copy(overseasAddress = None, changingAnswers = true))

        implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
          .withFormUrlEncodedBody("addressLine1" -> "Some address", "addressLine2" -> "line2", "countryCode" -> "GB")

        val result = await(controller.submitMainBusinessAddress(authenticatedRequest))

        status(result) shouldBe 200

        result should containMessages("error.country.invalid")

      }
    }
  }

}
