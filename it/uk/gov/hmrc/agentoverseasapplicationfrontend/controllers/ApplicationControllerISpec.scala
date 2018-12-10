package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers

import org.jsoup.Jsoup
import play.api.test.FakeRequest
import play.api.test.Helpers.{LOCATION, redirectLocation}
import uk.gov.hmrc.agentoverseasapplicationfrontend.models._

import uk.gov.hmrc.agentoverseasapplicationfrontend.support.BaseISpec
import uk.gov.hmrc.http.HeaderCarrier

class ApplicationControllerISpec extends BaseISpec {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val contactDetails = ContactDetails("test", "last", "senior agent", "12345", "test@email.com")
  private val amlsDetails = AmlsDetails("Keogh Chartered Accountants", Some("123456"))
  private val mainBusinessAddress = MainBusinessAddress("line 1", "line 2", None, None, countryCode = "IE")

  private val agentSession = AgentSession(
    amlsDetails = Some(amlsDetails),
    contactDetails = Some(contactDetails),
    tradingName = Some("some name"),
    mainBusinessAddress = Some(mainBusinessAddress)
  )

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
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(tradingName = None, mainBusinessAddress = None))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("tradingName" -> "test")

      val result = await(controller.submitTradingName(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showMainBusinessAddressForm().url

      val tradingName = await(sessionStoreService.fetchAgentSession).get.tradingName

      tradingName shouldBe Some("test")
    }
  }

  "GET /main-business-address" should {
    "display the trading address form" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(mainBusinessAddress = None))

      val result = await(controller.showMainBusinessAddressForm(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 200

      result should containMessages(
        "mainBusinessAddress.caption",
        "mainBusinessAddress.title"
      )
    }

    "redirect to /money-laundering when session not found" in {

      val authenticatedRequest = cleanCredsAgent(FakeRequest())

      val result = await(controller.showMainBusinessAddressForm(authenticatedRequest))

      status(result) shouldBe 303

      redirectLocation(result) shouldBe Some(routes.ApplicationController.showAntiMoneyLaunderingForm().url)
    }
  }

  "POST /main-business-address" should {
    "submit form and then redirect to registered-with-hmrc page" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(mainBusinessAddress = None))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("addressLine1" -> "line1", "addressLine2" -> "line2", "countryCode" -> "GB")

      val result = await(controller.submitMainBusinessAddress(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showRegisteredWithHmrcForm().url

      val tradingAddress = await(sessionStoreService.fetchAgentSession).get.mainBusinessAddress

      tradingAddress shouldBe Some(MainBusinessAddress("line1", "line2", None, None, "GB"))
    }
  }

  "GET /registered-with-hmrc" should {
    trait RegisteredWithHmrcSetup {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(registeredWithHmrc = None))
      val authenticatedRequest = cleanCredsAgent(FakeRequest())
      val result = await(controller.showRegisteredWithHmrcForm(authenticatedRequest))
    }

    "contain page titles and header content" in new RegisteredWithHmrcSetup {
      result should containMessages(
        "registeredWithHmrc.title",
        "registeredWithHmrc.caption",
        "registeredWithHmrc.form.title"
      )
    }

    "ask for whether they are registered with HMRC" in new RegisteredWithHmrcSetup {
      val doc = Jsoup.parse(bodyOf(result))

      val expectedRadios = Map(
        "yes" -> "registeredWithHmrc.form.registered.yes",
        "no" -> "registeredWithHmrc.form.registered.no",
        "unsure" -> "registeredWithHmrc.form.registered.unsure"
      )

      expectedRadios.foreach{
        case (expectedValue, expectedMessage) => {
          val elRadio = doc.getElementById(s"registeredWithHmrc-$expectedValue")
          elRadio should not be null
          elRadio.tagName() shouldBe "input"
          elRadio.attr("type") shouldBe "radio"
          elRadio.attr("value") shouldBe expectedValue

          checkMessageIsDefined(expectedMessage)
          val elLabel = doc.select(s"label[for=registeredWithHmrc-$expectedValue]").first()
          elLabel should not be null
          elLabel.text() shouldBe htmlEscapedMessage(expectedMessage)
        }
      }
    }

    "show existing selection if session already contains choice" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(registeredWithHmrc = Some(Unsure)))
      val authenticatedRequest = cleanCredsAgent(FakeRequest())
      val result = await(controller.showRegisteredWithHmrcForm(authenticatedRequest))
      val doc = Jsoup.parse(bodyOf(result))

      doc.getElementById("registeredWithHmrc-unsure").attr("checked") shouldBe "checked"
    }

    "contain a continue button" in new RegisteredWithHmrcSetup {
      result should containSubmitButton(
        expectedMessageKey = "button.continue",
        expectedElementId = "continue"
      )
    }

    "contain a back link to /main-business-address" in new RegisteredWithHmrcSetup {
      result should containLink(
        expectedMessageKey = "button.back",
        expectedHref = "/agent-services/apply-from-outside-uk/main-business-address"
      )
    }

    "contain a form that would POST to /registered-with-hmrc" in new RegisteredWithHmrcSetup {
      val doc = Jsoup.parse(bodyOf(result))

      val elForm = doc.select("form")
      elForm should not be null
      elForm.attr("action") shouldBe "/agent-services/apply-from-outside-uk/registered-with-hmrc"
      elForm.attr("method") shouldBe "POST"
    }

    "redirect to /money-laundering when session not found" in {
      val authenticatedRequest = cleanCredsAgent(FakeRequest())
      val result = await(controller.showRegisteredWithHmrcForm(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showAntiMoneyLaunderingForm().url
    }
  }

  "POST /registered-with-hmrc" should {
    "store choice in session after successful submission and redirect to next page" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(registeredWithHmrc = None))
      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("registeredWithHmrc" -> "yes")

      val result = await(controller.submitRegisteredWithHmrc(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showSelfAssessmentAgentCodeForm().url

      await(sessionStoreService.fetchAgentSession).get.registeredWithHmrc shouldBe Some(Yes)
    }

    "show validation error if no choice was selected" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(registeredWithHmrc = None))
      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())

      await(controller.submitRegisteredWithHmrc(authenticatedRequest)) should containMessages("error.required")

      await(sessionStoreService.fetchAgentSession).get.registeredWithHmrc shouldBe None
    }
  }
}
