package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers

import play.api.test.FakeRequest
import play.api.test.Helpers.{LOCATION, redirectLocation}
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.{AgentSession, AmlsDetails, ContactDetails, TradingAddress}
import uk.gov.hmrc.agentoverseasapplicationfrontend.support.BaseISpec
import uk.gov.hmrc.http.HeaderCarrier

class ApplicationControllerISpec extends BaseISpec {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val contactDetails = ContactDetails("test", "last", "senior agent", "12345", "test@email.com")
  private val amlsDetails = AmlsDetails("Keogh Chartered Accountants", Some("123456"))

  private val agentSession =
    AgentSession(Some(amlsDetails), contactDetails = Some(contactDetails), tradingName = Some("some name"))

  private lazy val controller: ApplicationController = app.injector.instanceOf[ApplicationController]

  "GET /money-laundering" should {
    "display the money-laundering form" in {
      val authenticatedRequest = cleanCredsAgent(FakeRequest())

      val result = await(controller.showAntiMoneyLaunderingForm(authenticatedRequest))

      status(result) shouldBe 200

      result should containMessages(
        "amls.title",
        "amls.inset.p1",
        "amls.form.supervisory_body",
        "amls.form.membership_number",
        "amls.hint.expandable",
        "amls.hint.expandable.p1"
      )

      result should containSubstrings("https://www.gov.uk/guidance/get-an-hmrc-agent-services-account",
        routes.SignOutController.signOut().url)
    }
  }

  "POST /money-laundering" should {
    "redirect to contact-details" in {
      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("amlsBody" -> "ABCD", "membershipNumber" -> "123445")

      val result = await(controller.submitAntiMoneyLaundering(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showContactDetailsForm().url

      val amlsDetails = await(sessionStoreService.fetchAgentSession).get.amlsDetails

      amlsDetails shouldBe Some(AmlsDetails("ABCD", Some("123445")))
    }
  }

  "GET /contact-details" should {
    "display the contact details form" in {

      await(sessionStoreService.cacheAgentSession(AgentSession(Some(AmlsDetails("body", Some("123"))), None)))

      val authenticatedRequest = cleanCredsAgent(FakeRequest())

      val result = await(controller.showContactDetailsForm(authenticatedRequest))

      status(result) shouldBe 200

      result should containMessages(
        "contactDetails.title",
        "contactDetails.form.firstName",
        "contactDetails.form.lastName",
        "contactDetails.form.jobTitle",
        "contactDetails.form.businessTelephone",
        "contactDetails.form.businessEmail"
      )
    }

    "redirect to /money-laundering when session not found" in {
      val authenticatedRequest = cleanCredsAgent(FakeRequest())

      val result = await(controller.showContactDetailsForm(authenticatedRequest))

      status(result) shouldBe 303

      redirectLocation(result) shouldBe Some(routes.ApplicationController.showAntiMoneyLaunderingForm().url)
    }
  }

  "POST /contact-details" should {
    "submit form and then redirect to trading-name" in {
      await(sessionStoreService.cacheAgentSession(AgentSession(Some(AmlsDetails("body", Some("123"))), None)))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("firstName" -> "test", "lastName" -> "last", "jobTitle" -> "senior agent", "businessTelephone" -> "12345", "businessEmail" -> "test@email.com")

      val result = await(controller.submitContactDetails(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showTradingNameForm().url

      val mayBeContactDetails = await(sessionStoreService.fetchAgentSession).get.contactDetails

      mayBeContactDetails shouldBe Some(ContactDetails("test", "last", "senior agent", "12345", "test@email.com"))
    }
  }

  "GET /trading-name" should {
    "display the trading name form" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(tradingName = None))

      val result = await(controller.showTradingNameForm(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 200

      result should containMessages(
        "tradingName.title",
        "tradingName.p1"
      )
    }

    "redirect to /money-laundering when session not found" in {

      val authenticatedRequest = cleanCredsAgent(FakeRequest())

      val result = await(controller.showTradingNameForm(authenticatedRequest))

      status(result) shouldBe 303

      redirectLocation(result) shouldBe Some(routes.ApplicationController.showAntiMoneyLaunderingForm().url)
    }
  }

  "POST /trading-name" should {
    "submit form and then redirect to main-business-details" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(tradingName = None))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("tradingName" -> "test")

      val result = await(controller.submitTradingName(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showTradingAddressForm().url

      val tradingName = await(sessionStoreService.fetchAgentSession).get.tradingName

      tradingName shouldBe Some("test")
    }
  }

  "GET /main-business-details" should {
    "display the trading address form" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(tradingAddress = None))

      val result = await(controller.showTradingAddressForm(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 200

      result should containMessages(
        "tradingAddress.caption",
        "tradingAddress.title"
      )
    }

    "redirect to /money-laundering when session not found" in {

      val authenticatedRequest = cleanCredsAgent(FakeRequest())

      val result = await(controller.showTradingAddressForm(authenticatedRequest))

      status(result) shouldBe 303

      redirectLocation(result) shouldBe Some(routes.ApplicationController.showAntiMoneyLaunderingForm().url)
    }
  }

  "POST /main-business-details" should {
    "submit form and then redirect to registered-with-hmrc page" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(tradingAddress = None))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("addressLine1" -> "line1", "addressLine2" -> "line2", "countryCode" -> "GB")

      val result = await(controller.submitTradingAddress(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showRegisteredWithHmrcForm().url

      val tradingAddress = await(sessionStoreService.fetchAgentSession).get.tradingAddress

      tradingAddress shouldBe Some(TradingAddress("line1", "line2", None, None, "GB"))
    }
  }
}
