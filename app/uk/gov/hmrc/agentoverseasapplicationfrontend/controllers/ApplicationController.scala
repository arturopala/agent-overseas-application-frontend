package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers

import javax.inject.{Inject, Singleton}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, Result}
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.auth.AgentAffinityNoHmrcAsAgentAuthAction
import uk.gov.hmrc.agentoverseasapplicationfrontend.forms._
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.{AgentSession, No, Unsure, Yes}
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.AgentSession.{IsRegisteredForUkTax, IsRegisteredWithHmrc}
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.AgentSession.IsRegisteredWithHmrc
import uk.gov.hmrc.agentoverseasapplicationfrontend.models._
import uk.gov.hmrc.agentoverseasapplicationfrontend.services.SessionStoreService
import uk.gov.hmrc.agentoverseasapplicationfrontend.utils.{CountryNamesLoader, toFuture}
import uk.gov.hmrc.agentoverseasapplicationfrontend.views.html._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.collection.immutable.SortedSet
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationController @Inject()(
  override val messagesApi: MessagesApi,
  val authConnector: AuthConnector,
  val env: Environment,
  validApplicantAction: AgentAffinityNoHmrcAsAgentAuthAction,
  val sessionStoreService: SessionStoreService,
  countryNamesLoader: CountryNamesLoader)(implicit val configuration: Configuration, ec: ExecutionContext)
    extends FrontendController with I18nSupport with CommonRouting {

  private val countries = countryNamesLoader.load
  private val validCountryCodes = countries.keys.toSet

  def showAntiMoneyLaunderingForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    val form = AmlsDetailsForm.form
    sessionStoreService.fetchAgentSession.map {
      case Some(session) =>
        Ok(anti_money_laundering(session.amlsDetails.fold(form)(form.fill)))

      case _ => Ok(anti_money_laundering(form))
    }
  }

  def submitAntiMoneyLaundering: Action[AnyContent] = validApplicantAction.async { implicit request =>
    AmlsDetailsForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => Ok(anti_money_laundering(formWithErrors)),
        validForm => {
          sessionStoreService.fetchAgentSession.flatMap {
            case Some(session) => updateSessionAndRedirect(session.copy(amlsDetails = Some(validForm)))
            case None          => updateSessionAndRedirect(AgentSession(Some(validForm)))
          }
        }
      )
  }

  def showContactDetailsForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      val form = ContactDetailsForm.form

      Ok(contact_details(session.contactDetails.fold(form)(form.fill)))
    }
  }

  def submitContactDetails: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      ContactDetailsForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => Ok(contact_details(formWithErrors)),
          validForm => updateSessionAndRedirect(session.copy(contactDetails = Some(validForm)))
        )
    }
  }

  def showTradingNameForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      val form = TradingNameForm.form

      Ok(trading_name(session.tradingName.fold(form)(form.fill)))
    }
  }

  def submitTradingName: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      TradingNameForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => Ok(trading_name(formWithErrors)),
          validForm => updateSessionAndRedirect(session.copy(tradingName = Some(validForm)))
        )
    }
  }

  def showMainBusinessAddressForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      val form = MainBusinessAddressForm.mainBusinessAddressForm(validCountryCodes)
      Ok(main_business_address(session.mainBusinessAddress.fold(form)(form.fill), countries))
    }
  }

  def submitMainBusinessAddress: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      MainBusinessAddressForm
        .mainBusinessAddressForm(validCountryCodes)
        .bindFromRequest()
        .fold(
          formWithErrors => Ok(main_business_address(formWithErrors, countries)),
          validForm => updateSessionAndRedirect(session.copy(mainBusinessAddress = Some(validForm)))
        )
    }
  }

  def showRegisteredWithHmrcForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      val form = RegisteredWithHmrcForm.form

      Ok(registered_with_hmrc(session.registeredWithHmrc.fold(form)(form.fill)))
    }
  }

  def submitRegisteredWithHmrc: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      RegisteredWithHmrcForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => Ok(registered_with_hmrc(formWithErrors)),
          validFormValue => updateSessionAndRedirect(session.copy(registeredWithHmrc = Some(validFormValue)))
        )
    }
  }

  def showAgentCodesForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      val form = AgentCodesForm.form

      Ok(self_assessment_agent_code(session.agentCodes.fold(form)(form.fill)))
    }
  }

  def submitAgentCodes: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      AgentCodesForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => Ok(self_assessment_agent_code(formWithErrors)),
          validFormValue => updateSessionAndRedirect(session.copy(agentCodes = Some(validFormValue)))
        )
    }
  }

  def showUkTaxRegistrationForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      val form = RegisteredForUkTaxForm.form
      val backLinkRoute: Call = ukTaxRegistrationBackLink(session)
      Ok(uk_tax_registration(session.registeredForUkTax.fold(form)(form.fill), backLinkRoute))
    }
  }

  def submitUkTaxRegistration: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      RegisteredForUkTaxForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            val backLinkRoute: Call = ukTaxRegistrationBackLink(session)
            Ok(uk_tax_registration(formWithErrors, backLinkRoute))
          },
          validFormValue => updateSessionAndRedirect(session.copy(registeredForUkTax = Some(validFormValue)))
        )
    }
  }

  def showPersonalDetailsForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      val form = PersonalDetailsForm.form

      Ok(personal_details(session.personalDetails.fold(form)(form.fill)))
    }
  }

  def submitPersonalDetails: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      PersonalDetailsForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => Ok(personal_details(formWithErrors)),
          validForm => {
            updateSessionAndRedirect(session.copy(personalDetails = Some(validForm)))
          }
        )
    }
  }

  def showCompanyRegistrationNumberForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      val form = CompanyRegistrationNumberForm.form
      Ok(
        company_registration_number(
          session.companyRegistrationNumber.fold(form)(form.fill),
          companyRegNumberBackLink(session)))
    }
  }

  def submitCompanyRegistrationNumber: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      CompanyRegistrationNumberForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => Ok(company_registration_number(formWithErrors, companyRegNumberBackLink(session))),
          validFormValue => updateSessionAndRedirect(session.copy(companyRegistrationNumber = Some(validFormValue)))
        )
    }
  }

  def showTaxRegistrationNumberForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { applicationSession =>
      val storedTrns = applicationSession.taxRegistrationNumbers.getOrElse(SortedSet.empty[String])

      val whichTrnToPopulate = if (storedTrns.size == 1) {
        storedTrns.headOption
      } else {
        None
      }

      val prePopulate = TaxRegistrationNumber(applicationSession.hasTaxRegNumbers, whichTrnToPopulate)
      Ok(tax_registration_number(TaxRegistrationNumberForm.form.fill(prePopulate)))
    }
  }

  def submitTaxRegistrationNumber: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { applicationData =>
      TaxRegistrationNumberForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => Ok(tax_registration_number(formWithErrors)),
          validForm =>
            updateSessionAndRedirect(
              applicationData.copy(
                hasTaxRegNumbers = validForm.canProvideTaxRegNo,
                taxRegistrationNumbers = validForm.value.flatMap(taxId => Some(SortedSet(taxId)))))
        )
    }
  }

  def showAddTaxRegNoForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { _ =>
      Ok(add_tax_registration_number(AddTrnForm.form))
    }
  }

  def submitAddTaxRegNo: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      AddTrnForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => Ok(add_tax_registration_number(formWithErrors)),
          validForm => {
            val trns = session.taxRegistrationNumbers match {
              case Some(numbers) => numbers + validForm
              case None          => SortedSet(validForm)
            }
            sessionStoreService
              .cacheAgentSession(session.copy(taxRegistrationNumbers = Some(trns)))
              .map(_ => Redirect(routes.ApplicationController.showYourTaxRegNumbersForm()))
          }
        )
    }
  }

  def showYourTaxRegNumbersForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { applicationSession =>
      val trns = applicationSession.taxRegistrationNumbers.getOrElse(SortedSet.empty)
      Ok(your_tax_registration_numbers(DoYouWantToAddAnotherTrnForm.form, trns))
    }
  }

  def submitYourTaxRegNumbers: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { applicationSession =>
      DoYouWantToAddAnotherTrnForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            val trns = applicationSession.taxRegistrationNumbers.getOrElse(SortedSet.empty)
            Ok(your_tax_registration_numbers(formWithErrors, trns))
          },
          validForm => {
            validForm.value match {
              case Some(true) => Redirect(routes.ApplicationController.showAddTaxRegNoForm().url)
              case _          => Redirect(routes.ApplicationController.showCheckYourAnswers().url)
            }
          }
        )
    }
  }

  def submitUpdateTaxRegNumber: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      UpdateTrnForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            Logger.warn(
              s"error during updating tax registration number ${formWithErrors.errors.map(_.message).mkString(",")}")
            InternalServerError
          },
          validForm =>
            validForm.updated match {
              case Some(updatedArn) =>
                val updatedSet = session.taxRegistrationNumbers.fold[SortedSet[String]](SortedSet.empty)(trns =>
                  trns - validForm.original + updatedArn)

                sessionStoreService
                  .cacheAgentSession(session.copy(taxRegistrationNumbers = Some(updatedSet)))
                  .map(_ => Redirect(routes.ApplicationController.showYourTaxRegNumbersForm()))

              case None =>
                Ok(
                  update_tax_registration_number(
                    UpdateTrnForm.form.fill(validForm.copy(updated = Some(validForm.original)))))
          }
        )
    }
  }

  def showCheckYourAnswers: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { applicationSession =>
      Ok("Success")
    }
  }

  private def ukTaxRegistrationBackLink(session: AgentSession) = Some(session) match {
    case IsRegisteredWithHmrc(Yes)         => routes.ApplicationController.showAgentCodesForm()
    case IsRegisteredWithHmrc(No | Unsure) => routes.ApplicationController.showRegisteredWithHmrcForm()
  }

  private def companyRegNumberBackLink(session: AgentSession) = Some(session) match {
    case IsRegisteredForUkTax(Yes)         => routes.ApplicationController.showPersonalDetailsForm().url
    case IsRegisteredForUkTax(No | Unsure) => routes.ApplicationController.showUkTaxRegistrationForm().url
  }

  private def withAgentSession(body: AgentSession => Future[Result])(implicit hc: HeaderCarrier): Future[Result] =
    sessionStoreService.fetchAgentSession.flatMap {
      case Some(session) => body(session)
      case None          => Redirect(routes.ApplicationController.showAntiMoneyLaunderingForm())
    }

  private def updateSessionAndRedirect(agentSession: AgentSession)(implicit hc: HeaderCarrier): Future[Result] =
    sessionStoreService.cacheAgentSession(agentSession).flatMap(_ => lookupNextPage.map(Redirect))
}
