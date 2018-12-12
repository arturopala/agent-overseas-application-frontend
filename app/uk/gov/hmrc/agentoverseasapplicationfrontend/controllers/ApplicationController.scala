package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers

import javax.inject.{Inject, Singleton}

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, Result}
import play.api.{Configuration, Environment}
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

  def showSelfAssessmentAgentCodeForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    NotImplemented
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
      val prePopulate = TaxRegistrationNumber(
        applicationSession.hasTaxRegNumbers,
        applicationSession.taxRegistrationNumbers.getOrElse(List.empty).headOption)

      val form = TaxRegistrationNumberForm.form.fill(prePopulate)

      Ok(tax_registration_number(form))
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
                taxRegistrationNumbers = validForm.value.flatMap(taxId => Some(List(taxId)))))
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
            val trn = session.taxRegistrationNumbers match {
              case Some(numbers) => numbers :+ validForm
              case None          => Seq(validForm)
            }
            sessionStoreService
              .cacheAgentSession(session.copy(taxRegistrationNumbers = Some(trn)))
              .map(_ => Redirect(routes.ApplicationController.showYourTaxRegNo))
          }
        )
    }
  }

  def showYourTaxRegNo: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { applicationSession =>
      NotImplemented
    }
  }

  def showCheckAnswers: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { applicationSession =>
      NotImplemented
    }
  }

  private def ukTaxRegistrationBackLink(session: AgentSession) = Some(session) match {
    case IsRegisteredWithHmrc(Yes)         => routes.ApplicationController.showSelfAssessmentAgentCodeForm()
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
