package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers

import org.jsoup.Jsoup
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{LOCATION, redirectLocation, flash}
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.PersonalDetails.RadioOption
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.PersonalDetails.RadioOption.SaUtrChoice
import uk.gov.hmrc.agentoverseasapplicationfrontend.models._
import uk.gov.hmrc.agentoverseasapplicationfrontend.stubs.AgentOverseasApplicationStubs
import uk.gov.hmrc.agentoverseasapplicationfrontend.support.BaseISpec
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http.HeaderCarrier

import scala.collection.immutable.SortedSet

class ApplicationControllerISpec extends BaseISpec with AgentOverseasApplicationStubs {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val contactDetails = ContactDetails("test", "last", "senior agent", "12345", "test@email.com")
  private val amlsDetails = AmlsDetails("Keogh Chartered Accountants", Some("123456"))
  private val mainBusinessAddress = MainBusinessAddress("line 1", "line 2", None, None, countryCode = "IE")
  private val personalDetails = PersonalDetails(Some(RadioOption.NinoChoice), Some(Nino("AB123456A")), None)

  private val agentSession = AgentSession(
    amlsDetails = Some(amlsDetails),
    contactDetails = Some(contactDetails),
    tradingName = Some("some name"),
    mainBusinessAddress = Some(mainBusinessAddress),
    personalDetails = Some(personalDetails)
  )

  private lazy val controller: ApplicationController = app.injector.instanceOf[ApplicationController]

  "GET /money-laundering" should {
    "redirect to it self when agentSession not initialised, should only be done once as auth action should initialise agentSession" in {
      given404OverseasApplications()
      val result = await(controller.showAntiMoneyLaunderingForm(cleanCredsAgent(FakeRequest())))

      redirectLocation(result).get shouldBe routes.ApplicationController.showAntiMoneyLaunderingForm().url
      await(sessionStoreService.fetchAgentSession).isDefined shouldBe true
    }

    "display the money-laundering form" in {
      given404OverseasApplications()
      await(sessionStoreService.cacheAgentSession(AgentSession()))
      val result = await(controller.showAntiMoneyLaunderingForm(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 200

      result should containMessages(
        "amls.title",
        "amls.inset.p1",
        "amls.form.supervisory_body",
        "amls.form.membership_number",
        "amls.hint.expandable",
        "amls.hint.expandable.p1"
      )

      result should containSubstrings(routes.SignOutController.signOut().url)
    }

    "display the money-laundering form with correct back button link when user is CHANGING ANSWERS" in {
      given404OverseasApplications()
      await(sessionStoreService.cacheAgentSession(AgentSession(changingAnswers = true)))
      val authenticatedRequest = cleanCredsAgent(FakeRequest())

      val result = await(controller.showAntiMoneyLaunderingForm(authenticatedRequest))

      status(result) shouldBe 200

      result should containLink("button.back", routes.ApplicationController.showCheckYourAnswers().url)
    }

    "display the money-laundering form with correct back button link when user is not changing answers and Has seen previously rejected application page" in {
      await(sessionStoreService.cacheAgentSession(AgentSession()))
      given200GetOverseasApplications(allRejected = true)

      val result = await(controller.showAntiMoneyLaunderingForm(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 200

      result should containLink("button.back", routes.StartController.applicationStatus().url)
    }
  }

  "POST /money-laundering" should {
    "redirect to contact-details" in {
      await(sessionStoreService.cacheAgentSession(AgentSession()))
      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("amlsBody" -> "Association of AccountingTechnicians (AAT)", "membershipNumber" -> "123445")

      val result = await(controller.submitAntiMoneyLaundering(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showContactDetailsForm().url

      val amlsDetails = await(sessionStoreService.fetchAgentSession).get.amlsDetails

      amlsDetails shouldBe Some(AmlsDetails("Association of AccountingTechnicians (AAT)", Some("123445")))
    }

    "redirect to check-your-answers if user is changing the details" in {
      //pre-state
      await(sessionStoreService.cacheAgentSession(AgentSession(changingAnswers = true)))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("amlsBody" -> "Association of AccountingTechnicians (AAT)", "membershipNumber" -> "123445")

      val result = await(controller.submitAntiMoneyLaundering(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showCheckYourAnswers().url

      val session = await(sessionStoreService.fetchAgentSession).get

      session.amlsDetails shouldBe Some(AmlsDetails("Association of AccountingTechnicians (AAT)", Some("123445")))

      //should revert to normal state after amending is successful
      session.changingAnswers shouldBe false
    }

    "show validation error when form params are incorrect" in {
      await(sessionStoreService.cacheAgentSession(AgentSession(changingAnswers = true)))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("amlsBody" -> "", "membershipNumber" -> "123445")

      val result = await(controller.submitAntiMoneyLaundering(authenticatedRequest))

      status(result) shouldBe 200

      result should containMessages("error.moneyLaunderingCompliance.amlsbody.empty")
    }
  }

  "GET /contact-details" should {
    "display the contact details form" in {

      await(
        sessionStoreService.cacheAgentSession(
          AgentSession(Some(AmlsDetails("body", Some("123"))), changingAnswers = true)))

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
      result should containSubstrings(routes.ApplicationController.showCheckYourAnswers().url)
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
        .withFormUrlEncodedBody(
          "firstName"         -> "test",
          "lastName"          -> "last",
          "jobTitle"          -> "senior agent",
          "businessTelephone" -> "12345",
          "businessEmail"     -> "test@email.com")

      val result = await(controller.submitContactDetails(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showTradingNameForm().url

      val mayBeContactDetails = await(sessionStoreService.fetchAgentSession).get.contactDetails

      mayBeContactDetails shouldBe Some(ContactDetails("test", "last", "senior agent", "12345", "test@email.com"))
    }

    "submit form and then redirect to check-your-answers if user is changing answers" in {
      //pre state
      await(
        sessionStoreService.cacheAgentSession(
          AgentSession(Some(AmlsDetails("body", Some("123"))), changingAnswers = true)))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody(
          "firstName"         -> "test",
          "lastName"          -> "last",
          "jobTitle"          -> "senior agent",
          "businessTelephone" -> "12345",
          "businessEmail"     -> "test@email.com")

      val result = await(controller.submitContactDetails(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showCheckYourAnswers().url

      val session = await(sessionStoreService.fetchAgentSession).get

      session.contactDetails shouldBe Some(ContactDetails("test", "last", "senior agent", "12345", "test@email.com"))

      //should revert to normal state after amending is successful
      session.changingAnswers shouldBe false
    }

    "show validation errors when form data is incorrect" in {
      //pre state
      await(
        sessionStoreService.cacheAgentSession(
          AgentSession(Some(AmlsDetails("body", Some("123"))), changingAnswers = true)))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody(
          "firstName"         -> "",
          "lastName"          -> "last**",
          "jobTitle"          -> "senior agent",
          "businessTelephone" -> "12345",
          "businessEmail"     -> "test")

      val result = await(controller.submitContactDetails(authenticatedRequest))

      status(result) shouldBe 200

      result should containMessages("error.firstName.blank",
        "error.lastName.invalid",
        "error.email")
    }
  }

  "GET /trading-name" should {
    "display the trading name form" in {
      sessionStoreService.currentSession.agentSession =
        Some(agentSession.copy(tradingName = None, changingAnswers = true))

      val result = await(controller.showTradingNameForm(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 200

      result should containMessages(
        "tradingName.title",
        "tradingName.p1"
      )

      result should containSubstrings(routes.ApplicationController.showCheckYourAnswers().url)
    }

    "redirect to /money-laundering when session not found" in {

      val authenticatedRequest = cleanCredsAgent(FakeRequest())

      val result = await(controller.showTradingNameForm(authenticatedRequest))

      status(result) shouldBe 303

      redirectLocation(result) shouldBe Some(routes.ApplicationController.showAntiMoneyLaunderingForm().url)
    }

    "prefill trading name if previously has used the endpoint POST /trading-name" in {
      sessionStoreService.currentSession.agentSession =
        Some(agentSession.copy(tradingName = Some("tradingName")))

      val result = await(controller.showTradingNameForm(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 200
      result should containMessages(
        "tradingName.title",
        "tradingName.p1"
      )

      val doc = Jsoup.parse(bodyOf(result))

      doc.getElementById("tradingName").attr("value") shouldBe "tradingName"
    }
  }

  "POST /trading-name" should {
    "submit form and then redirect to main-business-details" in {
      sessionStoreService.currentSession.agentSession =
        Some(agentSession.copy(tradingName = None, mainBusinessAddress = None))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("tradingName" -> "test")

      val result = await(controller.submitTradingName(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showMainBusinessAddressForm().url

      val tradingName = await(sessionStoreService.fetchAgentSession).get.tradingName

      tradingName shouldBe Some("test")
    }

    "submit form and then redirect to check-your-details if user is changing answers" in {
      sessionStoreService.currentSession.agentSession =
        Some(agentSession.copy(tradingName = None, mainBusinessAddress = None, changingAnswers = true))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("tradingName" -> "test")

      val result = await(controller.submitTradingName(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showCheckYourAnswers().url

      val session = await(sessionStoreService.fetchAgentSession).get

      session.tradingName shouldBe Some("test")

      //should revert to normal state after amending is successful
      session.changingAnswers shouldBe false
    }


    "show validation errors when form data is incorrect" in {
      sessionStoreService.currentSession.agentSession =
        Some(agentSession.copy(tradingName = None, mainBusinessAddress = None, changingAnswers = true))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("tradingName" -> "")

      val result = await(controller.submitTradingName(authenticatedRequest))

      status(result) shouldBe 200

      result should containMessages("error.tradingName.blank")
    }
  }

  "GET /main-business-address" should {
    "display the trading address form" in {
      sessionStoreService.currentSession.agentSession =
        Some(agentSession.copy(mainBusinessAddress = None, changingAnswers = true))

      val result = await(controller.showMainBusinessAddressForm(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 200

      result should containMessages(
        "mainBusinessAddress.caption",
        "mainBusinessAddress.title"
      )
      result should containSubstrings(routes.ApplicationController.showCheckYourAnswers().url)
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
        .withFormUrlEncodedBody("addressLine1" -> "line1", "addressLine2" -> "line2", "countryCode" -> "IE")

      val result = await(controller.submitMainBusinessAddress(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showRegisteredWithHmrcForm().url

      val tradingAddress = await(sessionStoreService.fetchAgentSession).get.mainBusinessAddress

      tradingAddress shouldBe Some(MainBusinessAddress("line1", "line2", None, None, "IE"))
    }

    "submit form and then redirect to check-your-answers page if user is changing answers" in {
      sessionStoreService.currentSession.agentSession =
        Some(agentSession.copy(mainBusinessAddress = None, changingAnswers = true))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("addressLine1" -> "line1", "addressLine2" -> "line2", "countryCode" -> "IE")

      val result = await(controller.submitMainBusinessAddress(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showCheckYourAnswers().url

      val session = await(sessionStoreService.fetchAgentSession).get

      session.mainBusinessAddress shouldBe Some(MainBusinessAddress("line1", "line2", None, None, "IE"))

      //should revert to normal state after amending is successful
      session.changingAnswers shouldBe false
    }

    "show validation errors when form data is incorrect" when {
      "address line 1 is blank" in {
        sessionStoreService.currentSession.agentSession =
          Some(agentSession.copy(mainBusinessAddress = None, changingAnswers = true))

        implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
          .withFormUrlEncodedBody("addressLine1" -> "", "addressLine2" -> "line2", "countryCode" -> "IE")

        val result = await(controller.submitMainBusinessAddress(authenticatedRequest))

        status(result) shouldBe 200

        result should containMessages("error.addressline.1.blank")
      }
      "country code is GB" in {
        sessionStoreService.currentSession.agentSession =
          Some(agentSession.copy(mainBusinessAddress = None, changingAnswers = true))

        implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
          .withFormUrlEncodedBody("addressLine1" -> "Some address", "addressLine2" -> "line2", "countryCode" -> "GB")

        val result = await(controller.submitMainBusinessAddress(authenticatedRequest))

        status(result) shouldBe 200

        result should containMessages("error.country.invalid")

      }
    }
  }

  "GET /registered-with-hmrc" should {
    class RegisteredWithHmrcSetup(agentSession: AgentSession = agentSession.copy(registeredWithHmrc = None)) {
      sessionStoreService.currentSession.agentSession = Some(agentSession)
      val authenticatedRequest = cleanCredsAgent(FakeRequest())
      val result = await(controller.showRegisteredWithHmrcForm(authenticatedRequest))
      val doc = Jsoup.parse(bodyOf(result))
    }

    "contain page titles and header content" in new RegisteredWithHmrcSetup {
      result should containMessages(
        "registeredWithHmrc.title",
        "registeredWithHmrc.caption",
        "registeredWithHmrc.form.title"
      )
    }

    "ask for whether they are registered with HMRC" in new RegisteredWithHmrcSetup {
      val expectedRadios = Map(
        "true"    -> "registeredWithHmrc.form.registered.yes",
        "false"     -> "registeredWithHmrc.form.registered.no"
      )

      expectedRadios.foreach {
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

    "show existing selection if session already contains choice" in
      new RegisteredWithHmrcSetup(agentSession.copy(registeredWithHmrc = Some(Yes))) {

        doc.getElementById("registeredWithHmrc-true").attr("checked") shouldBe "checked"
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

    "contain a back link to /check-your-details if user is changing answers" in new RegisteredWithHmrcSetup {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(changingAnswers = true))
      result should containLink(
        expectedMessageKey = "button.back",
        expectedHref = "/agent-services/apply-from-outside-uk/main-business-address"
      )
    }

    "contain a form that would POST to /registered-with-hmrc" in new RegisteredWithHmrcSetup {
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
      sessionStoreService.currentSession.agentSession =
        Some(agentSession.copy(registeredWithHmrc = None, changingAnswers = true))
      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("registeredWithHmrc" -> "true")

      val result = await(controller.submitRegisteredWithHmrc(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showAgentCodesForm().url

      val session = await(sessionStoreService.fetchAgentSession).get

      session.registeredWithHmrc shouldBe Some(Yes)

      session.changingAnswers shouldBe false
    }

    "show /check-your-answers page when the user in the amending state but clicks Continue without making any changes to the state" in {
      sessionStoreService.currentSession.agentSession =
        Some(agentSession.copy(registeredWithHmrc = Some(Yes), changingAnswers = true))
      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("registeredWithHmrc" -> "true")

      val result = await(controller.submitRegisteredWithHmrc(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showCheckYourAnswers().url

      val session = await(sessionStoreService.fetchAgentSession).get

      session.registeredWithHmrc shouldBe Some(Yes)

      session.changingAnswers shouldBe false
    }

    "show validation error if no choice was selected" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(registeredWithHmrc = None))
      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())

      await(controller.submitRegisteredWithHmrc(authenticatedRequest)) should containMessages("error.registeredWithHmrc.no-radio.selected")

      await(sessionStoreService.fetchAgentSession).get.registeredWithHmrc shouldBe None
    }
  }

  "GET /uk-tax-registration" should {
    val defaultAgentSession = agentSession.copy(
      registeredWithHmrc = Some(No),
      registeredForUkTax = None
    )
    class UkTaxRegistrationSetup(agentSession: AgentSession = defaultAgentSession) {
      sessionStoreService.currentSession.agentSession = Some(agentSession)
      val authenticatedRequest = cleanCredsAgent(FakeRequest())
      val result = await(controller.showUkTaxRegistrationForm(authenticatedRequest))
      val doc = Jsoup.parse(bodyOf(result))
    }

    "contain page titles and header content" in new UkTaxRegistrationSetup {
      result should containMessages(
        "ukTaxRegistration.title",
        "ukTaxRegistration.caption",
        "ukTaxRegistration.form.title"
      )
    }

    "ask for whether they are registered for UK tax" in new UkTaxRegistrationSetup {
      val expectedRadios = Map(
        "true"    -> "ukTaxRegistration.form.registered.yes",
        "false"     -> "ukTaxRegistration.form.registered.no"
      )

      expectedRadios.foreach {
        case (expectedValue, expectedMessage) => {
          val elRadio = doc.getElementById(s"registeredForUkTax-$expectedValue")
          elRadio should not be null
          elRadio.tagName() shouldBe "input"
          elRadio.attr("type") shouldBe "radio"
          elRadio.attr("value") shouldBe expectedValue

          checkMessageIsDefined(expectedMessage)
          val elLabel = doc.select(s"label[for=registeredForUkTax-$expectedValue]").first()
          elLabel should not be null
          elLabel.text() shouldBe htmlEscapedMessage(expectedMessage)
        }
      }
    }

    "show existing selection if session already contains choice" in
      new UkTaxRegistrationSetup(defaultAgentSession.copy(registeredForUkTax = Some(Yes))) {
        doc.getElementById("registeredForUkTax-true").attr("checked") shouldBe "checked"
      }

    "contain a continue button" in new UkTaxRegistrationSetup {
      result should containSubmitButton(
        expectedMessageKey = "button.continue",
        expectedElementId = "continue"
      )
    }

    "contain a back link to previous page" when {
      "previous page is /self-assessment-agent-code if they stated they are registered with HMRC" in
        new UkTaxRegistrationSetup(
          defaultAgentSession.copy(
            registeredWithHmrc = Some(Yes),
            agentCodes = Some(AgentCodes(None, None))
          )) {
          result should containLink(
            expectedMessageKey = "button.back",
            expectedHref = "/agent-services/apply-from-outside-uk/self-assessment-agent-code"
          )
        }

      "previous page is /registered-with-hmrc if they stated they are not registered with HMRC" in
        new UkTaxRegistrationSetup(
          defaultAgentSession.copy(
            registeredWithHmrc = Some(No),
            agentCodes = None
          )) {
          result should containLink(
            expectedMessageKey = "button.back",
            expectedHref = "/agent-services/apply-from-outside-uk/registered-with-hmrc"
          )
        }

      "previous page is /check-your-answers if user is changing answers" in
        new UkTaxRegistrationSetup(
          defaultAgentSession.copy(
            registeredWithHmrc = Some(No),
            agentCodes = None,
            changingAnswers = true
          )) {
          result should containLink(
            expectedMessageKey = "button.back",
            expectedHref = "/agent-services/apply-from-outside-uk/check-your-answers"
          )
        }
    }

    "contain a form that would POST to /uk-tax-registration" in new UkTaxRegistrationSetup {
      val elForm = doc.select("form")
      elForm should not be null
      elForm.attr("action") shouldBe "/agent-services/apply-from-outside-uk/uk-tax-registration"
      elForm.attr("method") shouldBe "POST"
    }

    "redirect to /money-laundering when session not found" in {
      val authenticatedRequest = cleanCredsAgent(FakeRequest())
      val result = await(controller.showUkTaxRegistrationForm(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showAntiMoneyLaunderingForm().url
    }
  }

  "POST /uk-tax-registration" should {
    "store choice in session after successful submission and redirect to next page" in {
      sessionStoreService.currentSession.agentSession = Some(
        agentSession.copy(
          registeredWithHmrc = Some(No),
          registeredForUkTax = None,
          personalDetails = None,
          changingAnswers = true
        ))
      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("registeredForUkTax" -> "true")

      val result = await(controller.submitUkTaxRegistration(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showPersonalDetailsForm().url

      val session = await(sessionStoreService.fetchAgentSession).get
      session.registeredForUkTax shouldBe Some(Yes)
      session.changingAnswers shouldBe false
    }

    "show /check-your-answers page when the user in the amending state but clicks Continue without making any changes to the state" in {
      sessionStoreService.currentSession.agentSession = Some(
        agentSession.copy(
          registeredWithHmrc = Some(No),
          registeredForUkTax = Some(No),
          personalDetails = None,
          changingAnswers = true
        ))
      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("registeredForUkTax" -> "false")

      val result = await(controller.submitUkTaxRegistration(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showCheckYourAnswers().url

      val session = await(sessionStoreService.fetchAgentSession).get
      session.registeredForUkTax shouldBe Some(No)
      session.changingAnswers shouldBe false
    }

    "show validation error if no choice was selected" in {
      sessionStoreService.currentSession.agentSession = Some(
        agentSession.copy(
          registeredWithHmrc = Some(No),
          registeredForUkTax = None
        ))
      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())

      await(controller.submitUkTaxRegistration(authenticatedRequest)) should containMessages("error.registeredForUkTaxForm.no-radio.selected")

      await(sessionStoreService.fetchAgentSession).get.registeredForUkTax shouldBe None
    }
  }

  "GET /personal-details" should {
    val defaultAgentSession = agentSession.copy(
      registeredWithHmrc = Some(No),
      registeredForUkTax = Some(Yes)
    )
    class PersonalDetailsSetup(agentSession: AgentSession = defaultAgentSession) {
      sessionStoreService.currentSession.agentSession = Some(agentSession)
      val authenticatedRequest = cleanCredsAgent(FakeRequest())
      val result = await(controller.showPersonalDetailsForm(authenticatedRequest))
      val doc = Jsoup.parse(bodyOf(result))
    }

    "contain page titles and header content" in new PersonalDetailsSetup {
      result should containMessages(
        "personalDetails.title",
        "personalDetails.p1",
        "personalDetails.p2",
        "personalDetails.form.nino",
        "personalDetails.form.input.label.nino",
        "personalDetails.form.helper.nino",
        "personalDetails.form.sautr",
        "personalDetails.form.input.label.sautr",
        "personalDetails.form.helper.sautr"
      )
    }

    "contain a back link to previous page - /uk-tax-registration" in new PersonalDetailsSetup {
      result should containLink(
        expectedMessageKey = "button.back",
        expectedHref = "/agent-services/apply-from-outside-uk/uk-tax-registration")
    }

    "contain a back link to check-your-answers if user is changing answers" in new PersonalDetailsSetup(
      defaultAgentSession.copy(changingAnswers = true)) {
      result should containLink(
        expectedMessageKey = "button.back",
        expectedHref = "/agent-services/apply-from-outside-uk/check-your-answers")
    }

    "contain a form that would POST to /personal-details" in new PersonalDetailsSetup {
      val elForm = doc.select("form")
      elForm should not be null
      elForm.attr("action") shouldBe "/agent-services/apply-from-outside-uk/personal-details"
      elForm.attr("method") shouldBe "POST"
    }

    "redirect to /money-laundering when session not found" in {
      val authenticatedRequest = cleanCredsAgent(FakeRequest())
      val result = await(controller.showUkTaxRegistrationForm(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showAntiMoneyLaunderingForm().url
    }
  }

  "POST /personal-details" should {
    "store choice in session after successful submission and redirect to next page" in {
      sessionStoreService.currentSession.agentSession = Some(
        agentSession.copy(
          registeredWithHmrc = Some(No),
          registeredForUkTax = Some(Yes),
          personalDetails = None
        ))
      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("personalDetailsChoice" -> "nino", "nino" -> "AB123456A", "saUtr" -> "")

      val result = await(controller.submitPersonalDetails(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showCompanyRegistrationNumberForm().url

      val savedPersonalDetails = await(sessionStoreService.fetchAgentSession).get.personalDetails.get
      savedPersonalDetails shouldBe PersonalDetails(Some(RadioOption.NinoChoice), Some(Nino("AB123456A")), None)
    }

    "store choice in session after successful submission and redirect check-your-answers if user is changing answers" in {
      sessionStoreService.currentSession.agentSession = Some(
        agentSession.copy(
          registeredWithHmrc = Some(No),
          registeredForUkTax = Some(Yes),
          personalDetails = None,
          changingAnswers = true
        ))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("personalDetailsChoice" -> "nino", "nino" -> "AB123456A", "saUtr" -> "")

      val result = await(controller.submitPersonalDetails(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showCheckYourAnswers().url

      val session = await(sessionStoreService.fetchAgentSession).get
      session.personalDetails.get shouldBe PersonalDetails(Some(RadioOption.NinoChoice), Some(Nino("AB123456A")), None)
      session.changingAnswers shouldBe false
    }

    "show validation error if no options are selected" in {
      sessionStoreService.currentSession.agentSession = Some(
        agentSession.copy(
          registeredWithHmrc = Some(No),
          registeredForUkTax = Some(Yes),
          personalDetails = None
        ))
      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("personalDetailsChoice" -> "", "nino" -> "", "saUtr" -> "")

      await(controller.submitPersonalDetails(authenticatedRequest)) should containMessages("error.personalDetails.no-radio.selected")
      await(sessionStoreService.fetchAgentSession).get.personalDetails shouldBe None
    }

    "show validation error if National Insurance number option is selected, but no value has been entered" in {
      sessionStoreService.currentSession.agentSession = Some(
        agentSession.copy(
          registeredWithHmrc = Some(No),
          registeredForUkTax = Some(Yes),
          personalDetails = None
        ))
      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("personalDetailsChoice" -> "nino", "nino" -> "", "saUtr" -> "")

      await(controller.submitPersonalDetails(authenticatedRequest)) should containMessages("error.nino.blank")
      await(sessionStoreService.fetchAgentSession).get.personalDetails shouldBe None
    }

    "show validation error if SA UTR option is selected, but no value has been entered" in {
      sessionStoreService.currentSession.agentSession = Some(
        agentSession.copy(
          registeredWithHmrc = Some(No),
          registeredForUkTax = Some(Yes),
          personalDetails = None
        ))
      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("personalDetailsChoice" -> "saUtr", "nino" -> "", "saUtr" -> "")

      await(controller.submitPersonalDetails(authenticatedRequest)) should containMessages("error.sautr.blank")
      await(sessionStoreService.fetchAgentSession).get.personalDetails shouldBe None
    }
  }

  "GET /tax-registration-number" should {
    val currentApplication: AgentSession = AgentSession(
      amlsDetails = Some(amlsDetails),
      contactDetails = Some(contactDetails),
      tradingName = Some("some name"),
      mainBusinessAddress = Some(mainBusinessAddress),
      registeredWithHmrc = Some(No),
      registeredForUkTax = Some(No),
      companyRegistrationNumber = Some(CompanyRegistrationNumber(Some(true), Some("ABC123")))
    )

    "page contains valid information on page the tax-registration-number page with form" in {
      sessionStoreService.currentSession.agentSession = Some(currentApplication)
      val authenticatedRequest = cleanCredsAgent(FakeRequest())

      val result = await(controller.showTaxRegistrationNumberForm(authenticatedRequest))
      val backButtonUrl = routes.ApplicationController.showCompanyRegistrationNumberForm().url

      status(result) shouldBe 200

      result should containMessages(
        "taxRegNo.title",
        "taxRegNo.p1",
        "taxRegNo.form.yes",
        "taxRegNo.form.yes.prompt",
        "taxRegNo.form.no",
        "button.back"
      )

      result should containSubstrings(backButtonUrl)
    }

    "if previously answered 'Yes' pre-populate form with 'Yes' and the value provided" in {
      val authenticatedRequest = cleanCredsAgent(FakeRequest())
      val taxRegNo = "tax_reg_number_123"
      sessionStoreService.currentSession.agentSession =
        Some(currentApplication.copy(hasTaxRegNumbers = Some(true), taxRegistrationNumbers = Some(SortedSet(taxRegNo))))

      val result = await(controller.showTaxRegistrationNumberForm(authenticatedRequest))

      status(result) shouldBe 200

      val doc = Jsoup.parse(bodyOf(result))

      bodyOf(result).contains(
        """<input id="canProvideTaxRegNo_true" type="radio" name="canProvideTaxRegNo" value="true" checked>""") shouldBe true
      doc.getElementById("canProvideTaxRegNo_true_value").attr("value") shouldBe taxRegNo
    }

    "if previously answered 'No' pre-populate form with checked 'No'" in {
      val authenticatedRequest = cleanCredsAgent(FakeRequest())
      sessionStoreService.currentSession.agentSession = Some(currentApplication.copy(hasTaxRegNumbers = Some(false)))

      val result = await(controller.showTaxRegistrationNumberForm(authenticatedRequest))

      status(result) shouldBe 200

      bodyOf(result).contains(
        """<input id="canProvideTaxRegNo_false" type="radio" name="canProvideTaxRegNo" value="false" checked>""") shouldBe true
    }
  }

  "POST /tax-registration-number" should {
    val currentApplication: AgentSession = AgentSession(
      amlsDetails = Some(amlsDetails),
      contactDetails = Some(contactDetails),
      tradingName = Some("some name"),
      mainBusinessAddress = Some(mainBusinessAddress),
      registeredWithHmrc = Some(No),
      registeredForUkTax = Some(No),
      companyRegistrationNumber = Some(CompanyRegistrationNumber(Some(true), Some("ABC123")))
    )

    "Provided selected 'Yes' on radioButton with included identifier, submit and redirect to next page /your-tax-registration-number" in {
      sessionStoreService.currentSession.agentSession = Some(currentApplication)
      val taxRegNo = "someTaxRegNo"
      val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("canProvideTaxRegNo" -> "true", "value" -> taxRegNo)

      val result = await(controller.submitTaxRegistrationNumber(authenticatedRequest))

      status(result) shouldBe 303

      redirectLocation(result) shouldBe Some(routes.ApplicationController.showYourTaxRegNumbersForm().url)
      val modifiedApplication = sessionStoreService.currentSession.agentSession.get

      modifiedApplication.hasTaxRegNumbers shouldBe Some(true)
      modifiedApplication.taxRegistrationNumbers shouldBe Some(Set(taxRegNo))

    }

    "Provided selected 'Yes' on radioButton without identifier, submit then show error message" in {
      sessionStoreService.currentSession.agentSession = Some(currentApplication)
      val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("canProvideTaxRegNo" -> "true")

      val result = await(controller.submitTaxRegistrationNumber(authenticatedRequest))

      status(result) shouldBe 200

      result should containSubstrings("This field is required")

      bodyOf(result).contains(
        """<input id="canProvideTaxRegNo_false" type="radio" name="canProvideTaxRegNo" value="false" checked>""") shouldBe false
      bodyOf(result).contains(
        """<input id="canProvideTaxRegNo_true" type="radio" name="canProvideTaxRegNo" value="true" checked>""") shouldBe true
    }

    "Provided selected 'No' on radioButton submit and redirect to next page /your-tax-registration-number" in {
      sessionStoreService.currentSession.agentSession = Some(currentApplication)
      val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("canProvideTaxRegNo" -> "false")

      val result = await(controller.submitTaxRegistrationNumber(authenticatedRequest))

      status(result) shouldBe 303

      redirectLocation(result) shouldBe Some(routes.ApplicationController.showCheckYourAnswers().url)
      val modifiedApplication = sessionStoreService.currentSession.agentSession.get

      modifiedApplication.hasTaxRegNumbers shouldBe Some(false)
      modifiedApplication.taxRegistrationNumbers shouldBe None
    }

    "Provided nothing selected on radio form submit and return with form error taxRegNo.form.no-radio.selected" in {
      sessionStoreService.currentSession.agentSession = Some(currentApplication)
      val authenticatedRequest = cleanCredsAgent(FakeRequest())

      val result = await(controller.submitTaxRegistrationNumber(authenticatedRequest))

      status(result) shouldBe 200

      result should containMessages("taxRegNo.form.no-radio.selected")
    }

    "show validation error when TRN is blank" in {
      sessionStoreService.currentSession.agentSession = Some(currentApplication)
      val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("canProvideTaxRegNo" -> "true", "value" -> "")

      val result = await(controller.submitTaxRegistrationNumber(authenticatedRequest))

      status(result) shouldBe 200

      result should containMessages("error.trn.blank")
    }
  }

  "GET /company-registration-number" should {
    "display the company-registration-number form" in {
      sessionStoreService.currentSession.agentSession =
        Some(agentSession.copy(registeredForUkTax = Some(Yes), companyRegistrationNumber = None))

      val result = await(controller.showCompanyRegistrationNumberForm(cleanCredsAgent(FakeRequest())))
      val backButtonUrl = routes.ApplicationController.showPersonalDetailsForm().url

      status(result) shouldBe 200
      result should containMessages(
        "companyRegistrationNumber.title",
        "companyRegistrationNumber.caption"
      )

      result should containSubstrings(backButtonUrl)
    }

    "display the company-registration-number form with check-your-answers back link if user is changing answers" in {
      sessionStoreService.currentSession.agentSession = Some(
        agentSession.copy(registeredForUkTax = Some(Yes), companyRegistrationNumber = None, changingAnswers = true))

      val result = await(controller.showCompanyRegistrationNumberForm(cleanCredsAgent(FakeRequest())))
      val backButtonUrl = routes.ApplicationController.showCheckYourAnswers().url

      status(result) shouldBe 200
      result should containMessages(
        "companyRegistrationNumber.title",
        "companyRegistrationNumber.caption"
      )

      result should containSubstrings(backButtonUrl)
    }

    "display the company-registration-number form with correct back button link in case user selects No option in the /uk-tax-registration page" in {
      sessionStoreService.currentSession.agentSession =
        Some(agentSession.copy(registeredForUkTax = Some(No),
          companyRegistrationNumber = Some(CompanyRegistrationNumber(Some(false), None))))

      val result = await(controller.showCompanyRegistrationNumberForm(cleanCredsAgent(FakeRequest())))
      val backButtonUrl = routes.ApplicationController.showUkTaxRegistrationForm().url

      status(result) shouldBe 200
      result should containMessages(
        "companyRegistrationNumber.title",
        "companyRegistrationNumber.caption"
      )

      result should containSubstrings(backButtonUrl)

      val doc = Jsoup.parse(bodyOf(result))

      doc.getElementById("confirmRegistration_false").attr("checked") shouldBe "checked"
    }
  }

  "POST /company-registration-number" should {
    "store choice in session after successful submission and redirect to next page" in {
      sessionStoreService.currentSession.agentSession = Some(
        agentSession.copy(
          companyRegistrationNumber = None,
          registeredWithHmrc = Some(No),
          registeredForUkTax = Some(Yes),
          agentCodes = None,
          personalDetails = Some(personalDetails)))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("confirmRegistration" -> "true", "registrationNumber" -> "AB123456")

      val result = await(controller.submitCompanyRegistrationNumber(authenticatedRequest))
      status(result) shouldBe 303

      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showTaxRegistrationNumberForm().url
      await(sessionStoreService.fetchAgentSession).get.companyRegistrationNumber shouldBe Some(
        CompanyRegistrationNumber(Some(true), Some("AB123456")))
    }

    "store choice in session after successful submission and redirect to check-your-answers page if user is changing answers" in {
      sessionStoreService.currentSession.agentSession = Some(
        agentSession.copy(
          companyRegistrationNumber = None,
          registeredWithHmrc = Some(No),
          registeredForUkTax = Some(Yes),
          agentCodes = None,
          personalDetails = Some(personalDetails),
          changingAnswers = true
        ))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("confirmRegistration" -> "true", "registrationNumber" -> "AB123456")

      val result = await(controller.submitCompanyRegistrationNumber(authenticatedRequest))
      status(result) shouldBe 303

      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showCheckYourAnswers().url

      val session = await(sessionStoreService.fetchAgentSession).get

      session.companyRegistrationNumber shouldBe Some(CompanyRegistrationNumber(Some(true), Some("AB123456")))
      session.changingAnswers shouldBe false
    }

    "show validation error if no choice was selected" in {
      sessionStoreService.currentSession.agentSession = Some(
        agentSession.copy(
          registeredWithHmrc = Some(No),
          registeredForUkTax = Some(No)
        ))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())

      await(controller.submitCompanyRegistrationNumber(authenticatedRequest)) should containMessages(
        "companyRegistrationNumber.error.no-radio.selected")
      await(sessionStoreService.fetchAgentSession).get.companyRegistrationNumber shouldBe None
    }

    "show validation error if Yes is selected but no input passed for registrationNumber" in {
      sessionStoreService.currentSession.agentSession = Some(
        agentSession.copy(
          registeredWithHmrc = Some(No),
          registeredForUkTax = Some(No)
        ))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("confirmRegistration" -> "true", "registrationNumber" -> "")

      await(controller.submitCompanyRegistrationNumber(authenticatedRequest)) should containMessages(
        "error.crn.blank")
      await(sessionStoreService.fetchAgentSession).get.companyRegistrationNumber shouldBe None
    }
  }

  "GET /add-tax-registration-number" should {
    "display the add-tax-registration-number form" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession)

      val result = await(controller.showAddTaxRegNoForm(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 200

      result should containMessages(
        "addTrn.title"
      )
    }

    "contain back button in the add-tax-registration-number form" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession)

      val result = await(controller.showAddTaxRegNoForm(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 200

      result should containLink(
        expectedMessageKey = "button.back",
        expectedHref = "/agent-services/apply-from-outside-uk/your-tax-registration-numbers"
      )
    }

    "redirect to /money-laundering when session not found" in {

      val authenticatedRequest = cleanCredsAgent(FakeRequest())

      val result = await(controller.showAddTaxRegNoForm(authenticatedRequest))

      status(result) shouldBe 303

      redirectLocation(result) shouldBe Some(routes.ApplicationController.showAntiMoneyLaunderingForm().url)
    }
  }

  "POST /add-tax-registration-number" should {
    "submit form and then redirect to your-tax-registration-number page" when {
      "current session has some tax reg. numbers" in {
        testSubmitAddTaxRegNo(Some(SortedSet("67890")))
      }
      "current session does not have any tax reg numbers" in {
        testSubmitAddTaxRegNo()
      }
    }

    def testSubmitAddTaxRegNo(numbers: Option[SortedSet[String]] = None) = {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(taxRegistrationNumbers = numbers))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("trn" -> "123456")

      val result = await(controller.submitAddTaxRegNo(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showYourTaxRegNumbersForm().url

      val taxRegNumbers = await(sessionStoreService.fetchAgentSession).get.taxRegistrationNumbers.get
      taxRegNumbers should contain("123456")
    }

    "show validation error when TRN is blank when submitting the form" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(taxRegistrationNumbers = Some(SortedSet.empty[String])))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("trn" -> "")

      val result = await(controller.submitAddTaxRegNo(authenticatedRequest))

      status(result) shouldBe 200

      result should containMessages("error.trn.blank")
    }
  }

  "GET /self-assessment-agent-code" should {
    val defaultAgentSession = agentSession.copy(
      registeredWithHmrc = Some(Yes),
      agentCodes = None
    )
    class UkTaxRegistrationSetup(agentSession: AgentSession = defaultAgentSession) {
      sessionStoreService.currentSession.agentSession = Some(agentSession)
      val authenticatedRequest = cleanCredsAgent(FakeRequest())
      val result = await(controller.showAgentCodesForm(authenticatedRequest))
      val doc = Jsoup.parse(bodyOf(result))
    }

    "contain page titles and header content" in new UkTaxRegistrationSetup {
      result should containMessages(
        "agentCodes.title",
        "agentCodes.caption",
        "agentCodes.p1",
        "agentCodes.form.hint"
      )
    }

    "ask for optional self-assessment and corporation-tax" in new UkTaxRegistrationSetup {
      Seq("self-assessment", "corporation-tax").foreach { agentCode =>
        result should containMessages(
          s"agentCodes.form.$agentCode.label",
          s"agentCodes.form.$agentCode.inset"
        )

        result should containElement(
          id = s"$agentCode-checkbox",
          tag = "input",
          attrs = Map(
            "type"  -> "checkbox",
            "name"  -> s"$agentCode-checkbox",
            "value" -> "true"
          )
        )

        result should containElement(
          id = agentCode,
          tag = "input",
          attrs = Map(
            "type" -> "text",
            "name" -> agentCode
          )
        )
      }
    }

    "show existing selection if session already contains choice" in
      new UkTaxRegistrationSetup(
        defaultAgentSession.copy(
          agentCodes = Some(AgentCodes(Some("saTestCode"), None))
        )) {
        result should containElement(
          id = "self-assessment-checkbox",
          tag = "input",
          attrs = Map("checked" -> "checked")
        )
      }

    "contain a continue button" in new UkTaxRegistrationSetup {
      result should containSubmitButton(
        expectedMessageKey = "button.continue",
        expectedElementId = "continue"
      )
    }

    "contain a back link to /registered-with-hmrc" in new UkTaxRegistrationSetup(
      defaultAgentSession.copy(
        registeredWithHmrc = Some(Yes)
      )) {
      result should containLink(
        expectedMessageKey = "button.back",
        expectedHref = "/agent-services/apply-from-outside-uk/registered-with-hmrc"
      )
    }

    "contain a back link to /check-your-answers if user is changing answers" in new UkTaxRegistrationSetup(
      defaultAgentSession.copy(
        registeredWithHmrc = Some(Yes),
        changingAnswers = true
      )) {
      result should containLink(
        expectedMessageKey = "button.back",
        expectedHref = "/agent-services/apply-from-outside-uk/check-your-answers"
      )
    }

    "contain a form that would POST to /self-assessment-agent-code" in new UkTaxRegistrationSetup {
      val elForm = doc.select("form")
      elForm should not be null
      elForm.attr("action") shouldBe "/agent-services/apply-from-outside-uk/self-assessment-agent-code"
      elForm.attr("method") shouldBe "POST"
    }

    "redirect to /money-laundering when session not found" in {
      val authenticatedRequest = cleanCredsAgent(FakeRequest())
      val result = await(controller.showUkTaxRegistrationForm(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showAntiMoneyLaunderingForm().url
    }
  }

  "POST /self-assessment-agent-code" should {
    "store choice in session after successful submission and redirect to next page" in {
      sessionStoreService.currentSession.agentSession = Some(
        agentSession.copy(
          registeredWithHmrc = Some(Yes),
          agentCodes = None,
          changingAnswers = true
        ))
      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody(
          "self-assessment-checkbox" -> "true",
          "self-assessment"          -> "SA1234",
          "corporation-tax-checkbox" -> "true",
          "corporation-tax"          -> "123456"
        )

      val result = await(controller.submitAgentCodes(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showCheckYourAnswers().url

      val session = await(sessionStoreService.fetchAgentSession).get

      session.agentCodes shouldBe Some(
        AgentCodes(
          Some("SA1234"),
          Some("123456")
        ))

      session.changingAnswers shouldBe false
    }

    "redirect to /uk-tax-registration if no agent code is selected" in {
      sessionStoreService.currentSession.agentSession = Some(
        agentSession.copy(
          registeredWithHmrc = Some(Yes),
          agentCodes = None,
          changingAnswers = true
        ))
      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody(
          "self-assessment" -> "",
          "corporation-tax" -> ""
        )
      val result = await(controller.submitAgentCodes(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showUkTaxRegistrationForm().url

      val session = await(sessionStoreService.fetchAgentSession).get
      session.agentCodes shouldBe Some(AgentCodes(None, None))
      session.changingAnswers shouldBe false
    }

    Seq("saAgentCode" -> "self-assessment", "ctAgentCode" -> "corporation-tax").foreach { case (key, value) =>
      s"show validation error if $value checkbox was selected but the text does not pass validation" in {

        sessionStoreService.currentSession.agentSession = Some(
          agentSession.copy(
            registeredWithHmrc = Some(Yes),
            agentCodes = None
          ))
        implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
          .withFormUrlEncodedBody(
            s"$value-checkbox" -> "true",
            "self-assessment"      -> "",
            "corporation-tax"      -> ""
          )

        await(controller.submitAgentCodes(authenticatedRequest)) should containMessages(s"error.$key.blank")

        await(sessionStoreService.fetchAgentSession).get.agentCodes shouldBe None
      }
    }
  }

  "GET /your-tax-registration-numbers" should {
    "display the /your-tax-registration-numbers page with DoYouWantToAddAnotherTrn form" in {
      sessionStoreService.currentSession.agentSession =
        Some(agentSession.copy(taxRegistrationNumbers = Some(SortedSet("123"))))

      val result = await(controller.showYourTaxRegNumbersForm(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 200

      result should containMessages(
        "yourTaxRegistrationNumbers.caption",
        "yourTaxRegistrationNumbers.title"
      )

      result should containLink(
        expectedMessageKey = "button.back",
        expectedHref = "/agent-services/apply-from-outside-uk/tax-registration-number"
      )
    }

    "display the /your-tax-registration-numbers page with /check-your-answers back link if user is changing answers" in {
      sessionStoreService.currentSession.agentSession =
        Some(agentSession.copy(taxRegistrationNumbers = Some(SortedSet("123")), changingAnswers = true))

      val result = await(controller.showYourTaxRegNumbersForm(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 200

      result should containMessages(
        "yourTaxRegistrationNumbers.caption",
        "yourTaxRegistrationNumbers.title"
      )

      result should containLink(
        expectedMessageKey = "button.back",
        expectedHref = "/agent-services/apply-from-outside-uk/check-your-answers"
      )
    }

    "display the /your-tax-registration-numbers page with correct text if there are no stored tax registration numbers found in session" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession)

      val result = await(controller.showYourTaxRegNumbersForm(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 200

      result should containSubstrings("You have added 0 tax registration numbers.")
    }

    "redirect to /money-laundering when session not found" in {

      val authenticatedRequest = cleanCredsAgent(FakeRequest())

      val result = await(controller.showYourTaxRegNumbersForm(authenticatedRequest))

      status(result) shouldBe 303

      redirectLocation(result) shouldBe Some(routes.ApplicationController.showAntiMoneyLaunderingForm().url)
    }
  }

  "POST /your-tax-registration-numbers" should {
    "submit form and then redirect to /add-tax-registration-number page if user selects true" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession)

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("value" -> "true")

      val result = await(controller.submitYourTaxRegNumbers(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showAddTaxRegNoForm().url
    }

    "submit form and then redirect to /check-answers page if user selects false" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession)

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("value" -> "false")

      val result = await(controller.submitYourTaxRegNumbers(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showCheckYourAnswers().url
    }

    "display the page with form errors if no radio button is selected" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession)

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())

      val result = await(controller.submitYourTaxRegNumbers(authenticatedRequest))

      status(result) shouldBe 200

      result should containMessages("doYouWantToAddAnotherTrn.error.no-radio.selected")
    }
  }

  "POST /update-tax-registration-number" should {

    "submit the form (with original and updated trns populated) and should correctly update the trn stored in the session" in {
      sessionStoreService.currentSession.agentSession =
        Some(agentSession.copy(taxRegistrationNumbers = Some(SortedSet("abc123"))))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("original" -> "abc123", "updated" -> "abc12345")

      val result = await(controller.submitUpdateTaxRegNumber(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showYourTaxRegNumbersForm().url
      sessionStoreService.fetchAgentSession.get.taxRegistrationNumbers shouldBe Some(Set("abc12345"))
    }

    "submit form and initially redirect to /update-tax-registration-number again with UpdateTrn form if 'updated' trn field is not set in the form" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession)

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("original" -> "ABC123")

      val result = await(controller.submitUpdateTaxRegNumber(authenticatedRequest))

      status(result) shouldBe 200
      result should containMessages("updateTrn.title")
    }
  }

  "GET remove-tax-registration-number/:trn" should {
    "display the remove-tax-registration-number form" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(taxRegistrationNumbers = Some(SortedSet("abc123"))))

      val result = await(controller.showRemoveTaxRegNumber("abc123")(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 200

      result should containMessages(
        "removeTrn.title",
        "removeTrn.form.yes",
        "removeTrn.form.no",
        "button.continue"
      )
    }

    "contain back button in the remove-tax-registration-number form" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(taxRegistrationNumbers = Some(SortedSet("abc123"))))

      val result = await(controller.showRemoveTaxRegNumber("abc123")(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 200

      result should containLink(
        expectedMessageKey = "button.back",
        expectedHref = "/agent-services/apply-from-outside-uk/your-tax-registration-numbers"
      )
    }

    "redirect to /money-laundering when session not found" in {
      val result = await(controller.showRemoveTaxRegNumber("abc123")(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 303

      redirectLocation(result) shouldBe Some(routes.ApplicationController.showAntiMoneyLaunderingForm().url)
    }

    "return 404 error page when the remove-tax-registration-number is called without trn" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(taxRegistrationNumbers = Some(SortedSet("abc123"))))

      val result = await(controller.showRemoveTaxRegNumber("")(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 200

      result should containMessages(
        "global.error.404.title",
        "global.error.404.heading",
        "global.error.404.message"
      )
    }
  }

  "POST /remove-tax-registration-number/:trn" should {

    "submit the form and should correctly remove the trn stored in the session & redirect to ask whether user does poses any taxRegNumber" in {
      sessionStoreService.currentSession.agentSession =
        Some(agentSession.copy(taxRegistrationNumbers = Some(SortedSet("abc123"))))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("isRemovingTrn" -> "true", "value" -> "abc123")

      val result = await(controller.submitRemoveTaxRegNumber("abc123")(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showTaxRegistrationNumberForm().url
      sessionStoreService.fetchAgentSession.get.taxRegistrationNumbers shouldBe None
      sessionStoreService.fetchAgentSession.get.hasTaxRegNumbers shouldBe None
    }

    "submit the form and should correctly remove the trn stored in the session" in {
      sessionStoreService.currentSession.agentSession =
        Some(agentSession.copy(taxRegistrationNumbers = Some(SortedSet("abc123", "anotherRegNumber"))))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("isRemovingTrn" -> "true", "value" -> "abc123")

      val result = await(controller.submitRemoveTaxRegNumber("abc123")(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showYourTaxRegNumbersForm().url
      sessionStoreService.fetchAgentSession.get.taxRegistrationNumbers shouldBe Some(SortedSet("anotherRegNumber"))
    }

    "redirect to showYourTaxRegNumbersForm page when the choice selected is No and the trn should not be removed" in {
      sessionStoreService.currentSession.agentSession =
        Some(agentSession.copy(taxRegistrationNumbers = Some(SortedSet("abc123"))))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("isRemovingTrn" -> "false", "value" -> "abc123")

      val result = await(controller.submitRemoveTaxRegNumber("abc123")(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showYourTaxRegNumbersForm().url
      sessionStoreService.fetchAgentSession.get.taxRegistrationNumbers shouldBe Some(SortedSet("abc123"))
    }

    "return validation error when the form is submitted without a choice selection" in {
      sessionStoreService.currentSession.agentSession =
        Some(agentSession.copy(taxRegistrationNumbers = Some(SortedSet("abc123"))))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("isRemovingTrn" -> "", "value" -> "abc123")

      val result = await(controller.submitRemoveTaxRegNumber("abc123")(authenticatedRequest))

      status(result) shouldBe 200

      result should containMessages("error.removeTrn.no-radio.selected")
    }
  }


  "GET /check-your-answers" should {

    def testAgentCodes(body: String, result: Boolean) = {
      body.contains("selfAssessmentCode") shouldBe result
      body.contains("corporationTaxCode") shouldBe result
    }

    def testMandatoryContent(result: Result) = {
      result should containMessages(
        "checkAnswers.title",
        "checkAnswers.change.button",
        "checkAnswers.amlsDetails.title",
        "checkAnswers.contactDetails.title",
        "checkAnswers.BusinessDetails.title",
        "checkAnswers.tradingName.title",
        "checkAnswers.mainBusinessAddress.title",
        "checkAnswers.registeredWithHmrc.title",
        "checkAnswers.confirm.p1",
        "checkAnswers.confirm.button")
    }

    "have correct back link" when {
      "provided agent codes journey branch, back link to show agent codes page" in {
        sessionStoreService.currentSession.agentSession = Some(
          AgentSession(
            Some(amlsDetails),
            Some(contactDetails),
            Some("tradingName"),
            Some(mainBusinessAddress),
            registeredWithHmrc = Some(Yes),
            agentCodes = Some(AgentCodes(Some("selfAssessmentCode"), Some("corporationTaxCode")))))

        val result = await(controller.showCheckYourAnswers(cleanCredsAgent(FakeRequest())))

        status(result) shouldBe 200
        result should containLink("button.back", routes.ApplicationController.showAgentCodesForm().url)
      }

      "no agent codes provided hence longer journey branch & any taxRegNumber/s provided, back link to your-tax-registration-numbers" in {
        sessionStoreService.currentSession.agentSession = Some(
          AgentSession(
            Some(amlsDetails),
            Some(contactDetails),
            Some("tradingName"),
            Some(mainBusinessAddress),
            registeredWithHmrc = Some(Yes),
            agentCodes = Some(AgentCodes(None,None)),
            registeredForUkTax = Some(No),
            companyRegistrationNumber = Some(CompanyRegistrationNumber(None, None)),
            hasTaxRegNumbers = Some(true),
            taxRegistrationNumbers = Some(SortedSet("someTaxRegNo"))))

        val result = await(controller.showCheckYourAnswers(cleanCredsAgent(FakeRequest())))

        status(result) shouldBe 200
        result should containLink("button.back", routes.ApplicationController.showYourTaxRegNumbersForm().url)
      }

      "no agent codes no taxRegNumbers provided, back link to ask if has tax-registration-number" in {
        sessionStoreService.currentSession.agentSession = Some(
          AgentSession(
            Some(amlsDetails),
            Some(contactDetails),
            Some("tradingName"),
            Some(mainBusinessAddress),
            registeredWithHmrc = Some(Yes),
            agentCodes = Some(AgentCodes(None,None)),
            registeredForUkTax = Some(No),
            companyRegistrationNumber = Some(CompanyRegistrationNumber(None, None)),
            hasTaxRegNumbers = Some(true),
            taxRegistrationNumbers = None))

        val result = await(controller.showCheckYourAnswers(cleanCredsAgent(FakeRequest())))

        status(result) shouldBe 200
        result should containLink("button.back", routes.ApplicationController.showTaxRegistrationNumberForm().url)
      }
    }

    "display the form with all data as expected when user goes through 'RegsiteredWithHmrc=Yes' flow" when {
      "agent codes are available" in {
        val registeredWithHmrc = Some(Yes)
        val agentCodes =
          AgentCodes(Some("selfAssessmentCode"), Some("corporationTaxCode"))

        sessionStoreService.currentSession.agentSession = Some(
          AgentSession(
            Some(amlsDetails),
            Some(contactDetails),
            Some("tradingName"),
            Some(mainBusinessAddress),
            registeredWithHmrc,
            Some(agentCodes)))

        val result = await(controller.showCheckYourAnswers(cleanCredsAgent(FakeRequest())))

        status(result) shouldBe 200

        testMandatoryContent(result)
        result should containMessages(

          "checkAnswers.agentCode.selfAssessment",
          "checkAnswers.agentCode.corporationTax"
        )

        result shouldNot containMessages(
          "checkAnswers.companyRegistrationNumber.title",
          "checkAnswers.taxRegistrationNumbers.title",
          "checkAnswers.registeredForUKTax.title",
          "checkAnswers.personalDetails.nino.title")

        testAgentCodes(bodyOf(result), true)
      }

      "agents codes are not available" when {
        "UkTaxRegistration is Yes" when {
          "CompanyRegistrationNumber is empty" in {
            val registeredWithHmrc = Some(Yes)
            val agentCodes =
              AgentCodes(None, None)

            sessionStoreService.currentSession.agentSession = Some(
              AgentSession(
                Some(amlsDetails),
                Some(contactDetails),
                Some("tradingName"),
                Some(mainBusinessAddress),
                registeredWithHmrc,
                Some(agentCodes),
                registeredForUkTax = Some(Yes),
                personalDetails = Some(PersonalDetails(Some(SaUtrChoice), None, Some(SaUtr("SA12345")))),
                companyRegistrationNumber = Some(CompanyRegistrationNumber(Some(false), None)),
                hasTaxRegNumbers = Some(false)))

            val result = await(controller.showCheckYourAnswers(cleanCredsAgent(FakeRequest())))

            status(result) shouldBe 200

            testMandatoryContent(result)
            result should containMessages(
              "checkAnswers.agentCode.title",
              "checkAnswers.agentCode.empty",
              "checkAnswers.companyRegistrationNumber.title",
              "checkAnswers.companyRegistrationNumber.empty"
            )

            testAgentCodes(bodyOf(result), false)
          }

          "CompanyRegistrationNumber is non empty" in {
            val registeredWithHmrc = Some(Yes)
            val agentCodes =
              AgentCodes(None, None)

            sessionStoreService.currentSession.agentSession = Some(
              AgentSession(
                Some(amlsDetails),
                Some(contactDetails),
                Some("tradingName"),
                Some(mainBusinessAddress),
                registeredWithHmrc,
                Some(agentCodes),
                registeredForUkTax = Some(Yes),
                personalDetails = Some(PersonalDetails(Some(SaUtrChoice), None, Some(SaUtr("SA12345")))),
                companyRegistrationNumber = Some(CompanyRegistrationNumber(Some(true), Some("999999"))),
                hasTaxRegNumbers = Some(false)))

            val result = await(controller.showCheckYourAnswers(cleanCredsAgent(FakeRequest())))

            status(result) shouldBe 200
            testMandatoryContent(result)

            result should containMessages(
              "checkAnswers.agentCode.title",
              "checkAnswers.agentCode.empty",
              "checkAnswers.companyRegistrationNumber.title"
            )

            testAgentCodes(bodyOf(result), false)
            bodyOf(result).contains("999999") shouldBe true
          }

          "TaxRegistrationNumbers is empty" in {
            val registeredWithHmrc = Some(Yes)
            val agentCodes= AgentCodes(None, None)

            sessionStoreService.currentSession.agentSession = Some(
              AgentSession(
                Some(amlsDetails),
                Some(contactDetails),
                Some("tradingName"),
                Some(mainBusinessAddress),
                registeredWithHmrc,
                Some(agentCodes),
                registeredForUkTax = Some(Yes),
                personalDetails = Some(PersonalDetails(Some(SaUtrChoice), None, Some(SaUtr("SA12345")))),
                companyRegistrationNumber = Some(CompanyRegistrationNumber(Some(false), None)),
                hasTaxRegNumbers = Some(false),
                taxRegistrationNumbers = None))

            val result = await(controller.showCheckYourAnswers(cleanCredsAgent(FakeRequest())))

            status(result) shouldBe 200
            testMandatoryContent(result)

            result should containMessages(
              "checkAnswers.agentCode.title",
              "checkAnswers.agentCode.empty",
              "checkAnswers.companyRegistrationNumber.title",
              "checkAnswers.companyRegistrationNumber.empty",
              "checkAnswers.taxRegistrationNumbers.title",
              "checkAnswers.taxRegistrationNumbers.empty"
            )

            testAgentCodes(bodyOf(result), false)
          }

          "TaxRegistrationNumbers is non empty" in {
            val registeredWithHmrc = Some(Yes)
            val agentCodes =
              AgentCodes(None, None)

            sessionStoreService.currentSession.agentSession = Some(
              AgentSession(
                Some(amlsDetails),
                Some(contactDetails),
                Some("tradingName"),
                Some(mainBusinessAddress),
                registeredWithHmrc,
                Some(agentCodes),
                registeredForUkTax = Some(Yes),
                personalDetails = Some(PersonalDetails(Some(SaUtrChoice), None, Some(SaUtr("SA12345")))),
                companyRegistrationNumber = Some(CompanyRegistrationNumber(Some(true), Some("123456"))),
                hasTaxRegNumbers = Some(true),
                taxRegistrationNumbers = Some(SortedSet("TX12345"))))

            val result = await(controller.showCheckYourAnswers(cleanCredsAgent(FakeRequest())))

            status(result) shouldBe 200

            testMandatoryContent(result)
            result should containMessages(
              "checkAnswers.registeredWithHmrc.title",
              "checkAnswers.agentCode.title",
              "checkAnswers.agentCode.empty",
              "checkAnswers.companyRegistrationNumber.title",
              "checkAnswers.taxRegistrationNumbers.title"
            )

            testAgentCodes(bodyOf(result), false)
            bodyOf(result).contains("TX12345") shouldBe true
          }
        }
        "UkTaxRegistration is No" in {
          val registeredWithHmrc = Some(Yes)
          val agentCodes =
            AgentCodes(None, None)

          sessionStoreService.currentSession.agentSession = Some(
            AgentSession(
              Some(amlsDetails),
              Some(contactDetails),
              Some("tradingName"),
              Some(mainBusinessAddress),
              registeredWithHmrc,
              Some(agentCodes),
              registeredForUkTax = Some(No),
              companyRegistrationNumber = Some(CompanyRegistrationNumber(Some(false), None)),
              hasTaxRegNumbers = Some(false)))

          val result = await(controller.showCheckYourAnswers(cleanCredsAgent(FakeRequest())))

          status(result) shouldBe 200

          testMandatoryContent(result)
          result should containMessages(
            "checkAnswers.title",
            "checkAnswers.change.button",
            "checkAnswers.amlsDetails.title",
            "checkAnswers.contactDetails.title",
            "checkAnswers.BusinessDetails.title",
            "checkAnswers.tradingName.title",
            "checkAnswers.mainBusinessAddress.title",
            "checkAnswers.registeredWithHmrc.title",
            "checkAnswers.agentCode.title",
            "checkAnswers.agentCode.empty"
          )

          result shouldNot containMessages("checkAnswers.personalDetails.nino.title")

          testAgentCodes(bodyOf(result), false)
        }
      }
    }

    "should display the form with all data as expected when user goes through 'RegsiteredWithHmrc=No' flow" in {
      val registeredWithHmrc = Some(No)

      sessionStoreService.currentSession.agentSession = Some(
        AgentSession(
          Some(amlsDetails),
          Some(contactDetails),
          Some("tradingName"),
          Some(mainBusinessAddress),
          registeredWithHmrc,
          registeredForUkTax = Some(Yes),
          personalDetails = Some(personalDetails),
          companyRegistrationNumber = Some(CompanyRegistrationNumber(Some(true), Some("crnCode"))),
          hasTaxRegNumbers = Some(true),
          taxRegistrationNumbers = Some(SortedSet("trn1", "trn2"))
        ))

      val result = await(controller.showCheckYourAnswers(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 200

      testMandatoryContent(result)
      result should containMessages(
        "checkAnswers.registeredForUKTax.title",
        "checkAnswers.personalDetails.nino.title",
        "checkAnswers.companyRegistrationNumber.title",
        "checkAnswers.taxRegistrationNumbers.title"
      )

      result shouldNot containMessages(
        "checkAnswers.agentCode.title",
        "checkAnswers.agentCode.empty")

      val body = bodyOf(result)

      body.contains("No") shouldBe true
      body.contains("Yes") shouldBe true
      body.contains("AB123456A") shouldBe true
      body.contains("crnCode") shouldBe true
      body.contains("trn1") shouldBe true
      body.contains("trn2") shouldBe true
    }
  }

  "POST /check-your-answers" should {

    def initialTestSetup = {
      val registeredWithHmrc = Some(Yes)
      val agentCodes =
        AgentCodes(Some("selfAssessmentCode"), Some("corporationTaxCode"))

      val agentSession =  AgentSession(
        amlsDetails = Some(amlsDetails),
        contactDetails = Some(contactDetails),
        tradingName = Some("tradingName"),
        mainBusinessAddress = Some(mainBusinessAddress),
        registeredWithHmrc = registeredWithHmrc,
        agentCodes = Some(agentCodes))

      sessionStoreService.currentSession.agentSession = Some(agentSession)
      agentSession
    }

    "submit the application and redirect to application-complete" in {
      val agentSession = initialTestSetup

      givenPostOverseasApplication(201, Json.toJson(CreateApplicationRequest(agentSession)).toString())

      val result = await(controller.submitCheckYourAnswers(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showApplicationComplete().url

      flash(result).get("tradingName").get shouldBe "tradingName"
      flash(result).get("contactDetail").get shouldBe "test@email.com"

      await(sessionStoreService.fetchAgentSession).isDefined shouldBe false
    }

    "return exception when agent-overseas-application backend is unavailable" in {
      val agentSession = initialTestSetup

      sessionStoreService.currentSession.agentSession = Some(agentSession)

      givenPostOverseasApplication(503, Json.toJson(CreateApplicationRequest(agentSession)).toString())

      an[Exception] should be thrownBy(await(controller.submitCheckYourAnswers(cleanCredsAgent(FakeRequest()))))
    }

    "redirect to /money-laundering when session not found" in {
      val result = await(controller.submitCheckYourAnswers()(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 303

      redirectLocation(result) shouldBe Some(routes.ApplicationController.showAntiMoneyLaunderingForm().url)
    }
  }

  "GET / application-complete" should {
    "should display the page data as expected" in {
      val tradingName = "testTradingName"
      val email = "testEmail@test.com"

      val result = await(controller.showApplicationComplete(basicAgentRequest(FakeRequest().withFlash("tradingName" -> tradingName,
        "contactDetail" -> email))))

      status(result) shouldBe 200
      result should containMessages(
        "applicationComplete.title",
          "applicationComplete.panel.body",
        "applicationComplete.whatHappensNext.heading",
        "applicationComplete.whatHappensNext.para2",
        "applicationComplete.whatHappensNext.para3",
        "applicationComplete.whatHappensNext.para4",
        "applicationComplete.whatYouCanDoNext.heading",
        "applicationComplete.whatYouCanDoNext.link",
        "applicationComplete.whatYouCanDoNext.text",
        "applicationComplete.help.heading",
        "applicationComplete.help.text",
        "applicationComplete.print",
        "applicationComplete.feedback.link",
        "applicationComplete.feedback.text"
      )

      bodyOf(result).contains(htmlEscapedMessage( "applicationComplete.whatHappensNext.para1", contactDetails.businessEmail))
      result should containSubstrings("We will send a confirmation email to",
        email,
        tradingName,
        routes.SignOutController.startFeedbackSurvey().url)
    }

    "303 to JOURNEY START when no required fields in flash, authAction should deal with routing circumstances" in {
      val result = await(controller.showApplicationComplete(basicAgentRequest(FakeRequest())))

      //as the Agent should have no agentSession at this point, previous last created application would be shown or start of Journey
      status(result) shouldBe 303
      redirectLocation(result).get shouldBe routes.ApplicationController.showAntiMoneyLaunderingForm().url
    }
  }
}
