package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers

import org.jsoup.Jsoup
import play.api.test.FakeRequest
import play.api.test.Helpers.{LOCATION, redirectLocation}
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.PersonalDetails.RadioOption
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.{AgentSession, AmlsDetails, CompanyRegistrationNumber, ContactDetails, Crn, FailureDetails, FileUploadStatus, MainBusinessAddress, No, PersonalDetails, Trn}
import uk.gov.hmrc.agentoverseasapplicationfrontend.stubs.AgentOverseasApplicationStubs
import uk.gov.hmrc.agentoverseasapplicationfrontend.support.BaseISpec
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier

import scala.collection.immutable.SortedSet

class TaxRegControllerISpec extends BaseISpec with AgentOverseasApplicationStubs {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val contactDetails = ContactDetails("test", "last", "senior agent", "12345", "test@email.com")
  private val amlsDetails = AmlsDetails("Keogh Chartered Accountants", Some("123456"))
  private val mainBusinessAddress = MainBusinessAddress("line 1", "line 2", None, None, countryCode = "IE")
  private val personalDetails = PersonalDetails(Some(RadioOption.NinoChoice), Some(Nino("AB123456A")), None)
  val failureDetails = FailureDetails("QUARANTINE","a virus was found!")
  val fileUploadStatus = FileUploadStatus("reference","READY",Some("filename"),Some(failureDetails))


  private val agentSession = AgentSession(
    amlsDetails = Some(amlsDetails),
    contactDetails = Some(contactDetails),
    tradingName = Some("some name"),
    mainBusinessAddress = Some(mainBusinessAddress),
    personalDetails = Some(personalDetails)
  )

  private lazy val controller: TaxRegController = app.injector.instanceOf[TaxRegController]


  "GET /tax-registration-number" should {
    val currentApplication: AgentSession = AgentSession(
      amlsDetails = Some(amlsDetails),
      contactDetails = Some(contactDetails),
      tradingName = Some("some name"),
      mainBusinessAddress = Some(mainBusinessAddress),
      registeredWithHmrc = Some(No),
      registeredForUkTax = Some(No),
      companyRegistrationNumber = Some(CompanyRegistrationNumber(Some(true), Some(Crn("ABC123"))))
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
      val taxRegNo = Trn("tax_reg_number_123")
      sessionStoreService.currentSession.agentSession =
        Some(currentApplication.copy(hasTaxRegNumbers = Some(true), taxRegistrationNumbers = Some(SortedSet(taxRegNo))))

      val result = await(controller.showTaxRegistrationNumberForm(authenticatedRequest))

      status(result) shouldBe 200

      val doc = Jsoup.parse(bodyOf(result))

      bodyOf(result).contains(
        """<input id="canProvideTaxRegNo_true" type="radio" name="canProvideTaxRegNo" value="true" checked>""") shouldBe true
      doc.getElementById("canProvideTaxRegNo_true_value").attr("value") shouldBe taxRegNo.value
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
      companyRegistrationNumber = Some(CompanyRegistrationNumber(Some(true), Some(Crn("ABC123"))))
    )

    "Provided selected 'Yes' on radioButton with included identifier, submit and redirect to next page /your-tax-registration-number" in {
      sessionStoreService.currentSession.agentSession = Some(currentApplication)
      val taxRegNo = Trn("someTaxRegNo")
      val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("canProvideTaxRegNo" -> "true", "value" -> taxRegNo.value)

      val result = await(controller.submitTaxRegistrationNumber(authenticatedRequest))

      status(result) shouldBe 303

      redirectLocation(result) shouldBe Some(routes.TaxRegController.showYourTaxRegNumbersForm().url)
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

    "Provided selected 'No' on radioButton submit and redirect to next page /more-information-needed" in {
      sessionStoreService.currentSession.agentSession = Some(currentApplication)
      val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("canProvideTaxRegNo" -> "false")

      val result = await(controller.submitTaxRegistrationNumber(authenticatedRequest))

      status(result) shouldBe 303

      redirectLocation(result) shouldBe Some(routes.TaxRegController.showMoreInformationNeeded().url)
      val modifiedApplication = sessionStoreService.currentSession.agentSession.get

      modifiedApplication.hasTaxRegNumbers shouldBe None
      modifiedApplication.taxRegistrationNumbers shouldBe None
      modifiedApplication.hasTrnsChanged shouldBe false
      modifiedApplication.trnUploadStatus shouldBe None
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

      redirectLocation(result) shouldBe Some(routes.AntiMoneyLaunderingController.showAntiMoneyLaunderingForm().url)
    }
  }

  "POST /add-tax-registration-number" should {
    "submit form and then redirect to your-tax-registration-number page" when {
      "current session has some tax reg. numbers" in {
        testSubmitAddTaxRegNo(Some(SortedSet(Trn("67890"))))
      }
      "current session does not have any tax reg numbers" in {
        testSubmitAddTaxRegNo()
      }
    }

    def testSubmitAddTaxRegNo(numbers: Option[SortedSet[Trn]] = None) = {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(taxRegistrationNumbers = numbers))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("trn" -> "123456")

      val result = await(controller.submitAddTaxRegNo(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.TaxRegController.showYourTaxRegNumbersForm().url

      val updatedSession = await(sessionStoreService.fetchAgentSession).get
      val taxRegNumbers =updatedSession.taxRegistrationNumbers.get
      taxRegNumbers should contain(Trn("123456"))
      updatedSession.hasTrnsChanged shouldBe true
    }

    "show validation error when TRN is blank when submitting the form" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(taxRegistrationNumbers = Some(SortedSet.empty[Trn])))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("trn" -> "")

      val result = await(controller.submitAddTaxRegNo(authenticatedRequest))

      status(result) shouldBe 200

      result should containMessages("error.trn.blank")
    }
  }


  "GET /your-tax-registration-numbers" should {
    "display the /your-tax-registration-numbers page with DoYouWantToAddAnotherTrn form" in {
      sessionStoreService.currentSession.agentSession =
        Some(agentSession.copy(taxRegistrationNumbers = Some(SortedSet(Trn("123")))))

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
        Some(agentSession.copy(taxRegistrationNumbers = Some(SortedSet(Trn("123"))), changingAnswers = true))

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

      redirectLocation(result) shouldBe Some(routes.AntiMoneyLaunderingController.showAntiMoneyLaunderingForm().url)
    }
  }

  "POST /your-tax-registration-numbers" should {
    "submit form and then redirect to /add-tax-registration-number page if user selects true" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession)

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("value" -> "true")

      val result = await(controller.submitYourTaxRegNumbers(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.TaxRegController.showAddTaxRegNoForm().url
    }

    "submit form and then redirect to /upload/trn page if user selects false" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(taxRegistrationNumbers = Some(SortedSet(Trn("12345"))), hasTaxRegNumbers = Some(true), hasTrnsChanged = true))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("value" -> "false")

      val result = await(controller.submitYourTaxRegNumbers(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.FileUploadController.showTrnUploadForm().url
    }

    "submit form and then redirect to /check-your-answers but NOT to /upload/trn page if user selects No and has not changed answers" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(taxRegistrationNumbers = Some(SortedSet(Trn("12345"))), hasTaxRegNumbers = Some(true), hasTrnsChanged = false))

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
        Some(agentSession.copy(taxRegistrationNumbers = Some(SortedSet(Trn("abc123")))))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("original" -> "abc123", "updated" -> "abc12345")

      val result = await(controller.submitUpdateTaxRegNumber(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.TaxRegController.showYourTaxRegNumbersForm().url
      sessionStoreService.fetchAgentSession.get.taxRegistrationNumbers shouldBe Some(SortedSet(Trn("abc12345")))
      sessionStoreService.fetchAgentSession.get.hasTrnsChanged shouldBe true
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
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(taxRegistrationNumbers = Some(SortedSet(Trn("abc123")))))

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
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(taxRegistrationNumbers = Some(SortedSet(Trn("abc123")))))

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

      redirectLocation(result) shouldBe Some(routes.AntiMoneyLaunderingController.showAntiMoneyLaunderingForm().url)
    }

    "return 404 error page when the remove-tax-registration-number is called without trn" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession.copy(taxRegistrationNumbers = Some(SortedSet(Trn("abc123")))))

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
        Some(agentSession.copy(taxRegistrationNumbers = Some(SortedSet(Trn("abc123")))))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("isRemovingTrn" -> "true", "value" -> "abc123")

      val result = await(controller.submitRemoveTaxRegNumber("abc123")(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.TaxRegController.showTaxRegistrationNumberForm().url
      sessionStoreService.fetchAgentSession.get.taxRegistrationNumbers shouldBe None
      sessionStoreService.fetchAgentSession.get.hasTaxRegNumbers shouldBe None
      sessionStoreService.fetchAgentSession.get.hasTrnsChanged shouldBe true
    }

    "submit the form and should correctly remove the trn stored in the session" in {
      sessionStoreService.currentSession.agentSession =
        Some(agentSession.copy(taxRegistrationNumbers = Some(SortedSet(Trn("abc123"), Trn("anotherRegNumber")))))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("isRemovingTrn" -> "true", "value" -> "abc123")

      val result = await(controller.submitRemoveTaxRegNumber("abc123")(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.TaxRegController.showYourTaxRegNumbersForm().url
      sessionStoreService.fetchAgentSession.get.taxRegistrationNumbers shouldBe Some(SortedSet(Trn("anotherRegNumber")))
      sessionStoreService.fetchAgentSession.get.hasTrnsChanged shouldBe true
    }

    "redirect to showYourTaxRegNumbersForm page when the choice selected is No and the trn should not be removed" in {
      sessionStoreService.currentSession.agentSession =
        Some(agentSession.copy(taxRegistrationNumbers = Some(SortedSet(Trn("abc123")))))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("isRemovingTrn" -> "false", "value" -> "abc123")

      val result = await(controller.submitRemoveTaxRegNumber("abc123")(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.TaxRegController.showYourTaxRegNumbersForm().url
      sessionStoreService.fetchAgentSession.get.taxRegistrationNumbers shouldBe Some(SortedSet(Trn("abc123")))
      sessionStoreService.fetchAgentSession.get.hasTrnsChanged shouldBe false
    }

    "return validation error when the form is submitted without a choice selection" in {
      sessionStoreService.currentSession.agentSession =
        Some(agentSession.copy(taxRegistrationNumbers = Some(SortedSet(Trn("abc123")))))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("isRemovingTrn" -> "", "value" -> "abc123")

      val result = await(controller.submitRemoveTaxRegNumber("abc123")(authenticatedRequest))

      status(result) shouldBe 200

      result should containMessages("error.removeTrn.no-radio.selected")
    }
  }

  "GET /more-information-needed" should {
    "return page with expected content" in {
      sessionStoreService.currentSession.agentSession = Some(agentSession)
      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())

      val result = await(controller.showMoreInformationNeeded()(authenticatedRequest))

      status(result) shouldBe 200

      containMessages(
        "taxRegNo.more_info_required.title",
        "taxRegNo.more_info_required.p1",
        "taxRegNo.more_info_required.p2"
      )
    }
  }

}
