package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers

import org.jsoup.Jsoup
import play.api.test.FakeRequest
import play.api.test.Helpers.{LOCATION, redirectLocation}
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.PersonalDetails.RadioOption
import uk.gov.hmrc.agentoverseasapplicationfrontend.models._
import uk.gov.hmrc.agentoverseasapplicationfrontend.support.BaseISpec
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier

class ApplicationControllerISpec extends BaseISpec {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val contactDetails = ContactDetails("test", "last", "senior agent", "12345", "test@email.com")
  private val amlsDetails = AmlsDetails("Keogh Chartered Accountants", Some("123456"))
  private val mainBusinessAddress = MainBusinessAddress("line 1", "line 2", None, None, countryCode = "IE")
  private val personalDetails = PersonalDetails(RadioOption.NinoChoice, Some(Nino("AB123456A")), None)

  private val agentSession = AgentSession(
    amlsDetails = Some(amlsDetails),
    contactDetails = Some(contactDetails),
    tradingName = Some("some name"),
    mainBusinessAddress = Some(mainBusinessAddress),
    personalDetails = Some(personalDetails)
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

    "show existing selection if session already contains choice" in
      new RegisteredWithHmrcSetup(agentSession.copy(registeredWithHmrc = Some(Unsure))) {

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
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showAgentCodesForm().url

      await(sessionStoreService.fetchAgentSession).get.registeredWithHmrc shouldBe Some(Yes)
    }

    "show validation error if no choice was selected" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(registeredWithHmrc = None))
      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())

      await(controller.submitRegisteredWithHmrc(authenticatedRequest)) should containMessages("error.required")

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
        "yes" -> "ukTaxRegistration.form.registered.yes",
        "no" -> "ukTaxRegistration.form.registered.no",
        "unsure" -> "ukTaxRegistration.form.registered.unsure"
      )

      expectedRadios.foreach{
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
      new UkTaxRegistrationSetup(defaultAgentSession.copy(registeredForUkTax = Some(Unsure))) {
      doc.getElementById("registeredForUkTax-unsure").attr("checked") shouldBe "checked"
    }

    "contain a continue button" in new UkTaxRegistrationSetup {
      result should containSubmitButton(
        expectedMessageKey = "button.continue",
        expectedElementId = "continue"
      )
    }

    "contain a back link to previous page" when {
      "previous page is /self-assessment-agent-code if they stated they are registered with HMRC" in
        new UkTaxRegistrationSetup(defaultAgentSession.copy(
          registeredWithHmrc = Some(Yes),
          agentCodes = Some(AgentCodes(None, None, None, None))
        )) {
        result should containLink(
          expectedMessageKey = "button.back",
          expectedHref = "/agent-services/apply-from-outside-uk/self-assessment-agent-code"
        )
      }
      "previous page is /registered-with-hmrc if they stated they are not registered with HMRC" in
        new UkTaxRegistrationSetup(defaultAgentSession.copy(
          registeredWithHmrc = Some(No),
          agentCodes = None
        )) {
        result should containLink(
          expectedMessageKey = "button.back",
          expectedHref = "/agent-services/apply-from-outside-uk/registered-with-hmrc"
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
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(
        registeredWithHmrc = Some(No),
        registeredForUkTax = None,
        personalDetails = None
      ))
      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("registeredForUkTax" -> "yes")

      val result = await(controller.submitUkTaxRegistration(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showPersonalDetailsForm().url

      await(sessionStoreService.fetchAgentSession).get.registeredForUkTax shouldBe Some(Yes)
    }

    "show validation error if no choice was selected" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(
        registeredWithHmrc = Some(No),
        registeredForUkTax = None
      ))
      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())

      await(controller.submitUkTaxRegistration(authenticatedRequest)) should containMessages("error.required")

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
        "personalDetails.form.helper.sautr",
        "personalDetails.form.noDetails"
      )
    }

    "contain a back link to previous page - /uk-tax-registration " in new PersonalDetailsSetup {
      result should containLink(
        expectedMessageKey = "button.back",
        expectedHref = "/agent-services/apply-from-outside-uk/uk-tax-registration")
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
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(
        registeredWithHmrc = Some(No),
        registeredForUkTax = Some(Yes)
      ))
      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("personalDetailsChoice" -> "nino", "nino" -> "AB123456A", "saUtr" -> "")

      val result = await(controller.submitPersonalDetails(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showCompanyRegistrationNumberForm().url

      val savedPersonalDetails = await(sessionStoreService.fetchAgentSession).get.personalDetails.get
      savedPersonalDetails shouldBe PersonalDetails(RadioOption.NinoChoice, Some(Nino("AB123456A")), None)
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
      sessionStoreService.currentSession.agentSession = Some(currentApplication.copy(hasTaxRegNumbers = Some(true),
        taxRegistrationNumbers = Some(List(taxRegNo))))

      val result = await(controller.showTaxRegistrationNumberForm(authenticatedRequest))

      status(result) shouldBe 200

      val doc = Jsoup.parse(bodyOf(result))

      bodyOf(result).contains("""<input id="canProvideTaxRegNo_true" type="radio" name="canProvideTaxRegNo" value="true" checked>""") shouldBe true
      doc.getElementById("canProvideTaxRegNo_true_value").attr("value") shouldBe taxRegNo
    }

    "if previously answered 'No' pre-populate form with checked 'No'" in {
      val authenticatedRequest = cleanCredsAgent(FakeRequest())
      sessionStoreService.currentSession.agentSession = Some(currentApplication.copy(hasTaxRegNumbers = Some(false)))

      val result = await(controller.showTaxRegistrationNumberForm(authenticatedRequest))

      status(result) shouldBe 200

      bodyOf(result).contains("""<input id="canProvideTaxRegNo_false" type="radio" name="canProvideTaxRegNo" value="false" checked>""") shouldBe true
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

      redirectLocation(result) shouldBe Some(routes.ApplicationController.showYourTaxRegNo().url)
      val modifiedApplication = sessionStoreService.currentSession.agentSession.get

      modifiedApplication.hasTaxRegNumbers shouldBe Some(true)
      modifiedApplication.taxRegistrationNumbers shouldBe Some(List(taxRegNo))

    }

    "Provided selected 'Yes' on radioButton without identifier, submit then show 'This field is required' error message" in {
      sessionStoreService.currentSession.agentSession = Some(currentApplication)
      val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("canProvideTaxRegNo" -> "true")

      val result = await(controller.submitTaxRegistrationNumber(authenticatedRequest))

      status(result) shouldBe 200

      result should containSubstrings("This field is required")

      bodyOf(result).contains("""<input id="canProvideTaxRegNo_false" type="radio" name="canProvideTaxRegNo" value="false" checked>""") shouldBe false
      bodyOf(result).contains("""<input id="canProvideTaxRegNo_true" type="radio" name="canProvideTaxRegNo" value="true" checked>""") shouldBe true
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
  }

  "GET /company-registration-number" should {
    "display the company-registration-number form" in {
      sessionStoreService.currentSession.agentSession =
        Some(agentSession.copy(
          registeredForUkTax = Some(Yes),
          companyRegistrationNumber = None))

      val result = await(controller.showCompanyRegistrationNumberForm(cleanCredsAgent(FakeRequest())))
      val backButtonUrl = routes.ApplicationController.showPersonalDetailsForm().url

      status(result) shouldBe 200
      result should containMessages(
        "companyRegistrationNumber.title",
        "companyRegistrationNumber.caption"
      )

      result should containSubstrings(backButtonUrl)
    }

    "display the company-registration-number form with correct back button link in case user selects No|Unsure option in the /uk-tax-registration page" in {
      sessionStoreService.currentSession.agentSession =
        Some(agentSession.copy(
          registeredForUkTax = Some(No),
          companyRegistrationNumber = None))

      val result = await(controller.showCompanyRegistrationNumberForm(cleanCredsAgent(FakeRequest())))
      val backButtonUrl = routes.ApplicationController.showUkTaxRegistrationForm().url

      status(result) shouldBe 200
      result should containMessages(
        "companyRegistrationNumber.title",
        "companyRegistrationNumber.caption"
      )

      result should containSubstrings(backButtonUrl)
    }
  }

  "POST /company-registration-number" should {
    "store choice in session after successful submission and redirect to next page" in {
      sessionStoreService.currentSession.agentSession =
        Some(agentSession.copy(companyRegistrationNumber = None,
          registeredWithHmrc = Some(No),
          registeredForUkTax = Some(Yes),
          agentCodes = None,
          personalDetails = Some(personalDetails)))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("confirmRegistration" -> "true", "registrationNumber" -> "ABC123")

      val result = await(controller.submitCompanyRegistrationNumber(authenticatedRequest))
      status(result) shouldBe 303

      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showTaxRegistrationNumberForm().url
      await(sessionStoreService.fetchAgentSession).get.companyRegistrationNumber shouldBe Some(CompanyRegistrationNumber(Some(true), Some("ABC123")))
    }

    "show validation error if no choice was selected" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(
        registeredWithHmrc = Some(No),
        registeredForUkTax = Some(No)
      ))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())

      await(controller.submitCompanyRegistrationNumber(authenticatedRequest)) should containMessages("companyRegistrationNumber.error.no-radio.selected")
      await(sessionStoreService.fetchAgentSession).get.companyRegistrationNumber shouldBe None
    }

    "show validation error if Yes is selected but no input passed for registrationNumber" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(
        registeredWithHmrc = Some(No),
        registeredForUkTax = Some(No)
      ))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("confirmRegistration" -> "true", "registrationNumber" -> "")

      await(controller.submitCompanyRegistrationNumber(authenticatedRequest)) should containSubstrings("This field is required")
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
        expectedHref = "/agent-services/apply-from-outside-uk/your-tax-registration-number"
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
        testSubmitAddTaxRegNo(Some(Seq("67890")))
      }
      "current session does not have any tax reg numbers" in {
        testSubmitAddTaxRegNo()
      }
    }

    def testSubmitAddTaxRegNo(numbers: Option[Seq[String]] = None) = {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(taxRegistrationNumbers = numbers))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("trn" -> "123456")

      val result = await(controller.submitAddTaxRegNo(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showYourTaxRegNo().url

      val taxRegNumbers = await(sessionStoreService.fetchAgentSession).get.taxRegistrationNumbers.get
      taxRegNumbers should contain("123456")
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

    "ask for optional self-assessment, corporation-tax, vat, and paye agent codes" in new UkTaxRegistrationSetup {
      Seq("self-assessment", "corporation-tax", "vat",  "paye").foreach{ agentCode =>
        result should containMessages(
          s"agentCodes.form.$agentCode.label",
          s"agentCodes.form.$agentCode.inset"
        )

        result should containElement(
          id = s"$agentCode-checkbox",
          tag = "input",
          attrs = Map(
            "type" -> "checkbox",
            "name" -> s"$agentCode-checkbox",
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
      new UkTaxRegistrationSetup(defaultAgentSession.copy(
        agentCodes = Some(AgentCodes(Some("saTestCode"), None, None, None))
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

    "contain a back link to /registered-with-hmrc" in new UkTaxRegistrationSetup(defaultAgentSession.copy(
      registeredWithHmrc = Some(Yes)
    )) {
      result should containLink(
        expectedMessageKey = "button.back",
        expectedHref = "/agent-services/apply-from-outside-uk/registered-with-hmrc"
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
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(
        registeredWithHmrc = Some(Yes),
        agentCodes = None
      ))
      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody(
          "self-assessment-checkbox" -> "true",
          "self-assessment" -> "saTestCode",
          "corporation-tax-checkbox" -> "true",
          "corporation-tax" -> "ctTestCode",
          "vat-checkbox" -> "true",
          "vat" -> "vatTestCode",
          "paye-checkbox" -> "true",
          "paye" -> "payeTestCode"
        )

      val result = await(controller.submitAgentCodes(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showCheckYourAnswers().url

      await(sessionStoreService.fetchAgentSession).get.agentCodes shouldBe Some(AgentCodes(
        Some("saTestCode"),
        Some("ctTestCode"),
        Some("vatTestCode"),
        Some("payeTestCode")
      ))
    }

    "redirect to /uk-tax-registration if no agent code is selected" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(
        registeredWithHmrc = Some(Yes),
        agentCodes = None
      ))
      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody(
          "self-assessment" -> "",
          "corporation-tax" -> "",
          "vat" -> "",
          "paye" -> ""
        )
      val result = await(controller.submitAgentCodes(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showUkTaxRegistrationForm().url

      await(sessionStoreService.fetchAgentSession).get.agentCodes shouldBe Some(AgentCodes(None, None, None, None))
    }

    Seq("self-assessment", "corporation-tax", "vat", "paye" ).foreach { agentCode =>
      s"show validation error if $agentCode checkbox was selected but the text does not pass validation" in {

        sessionStoreService.currentSession.agentSession = Some(agentSession.copy(
          registeredWithHmrc = Some(Yes),
          agentCodes = None
        ))
        implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
          .withFormUrlEncodedBody(
            s"$agentCode-checkbox" -> "true",
            "self-assessment" -> "",
            "corporation-tax" -> "",
            "vat" -> "",
            "paye" -> ""
          )

        await(controller.submitAgentCodes(authenticatedRequest)) should containMessages("error.required")

        await(sessionStoreService.fetchAgentSession).get.agentCodes shouldBe None
      }
    }
  }
}
