package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers

import javax.inject.{Inject, Singleton}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Result}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.auth.AgentAffinityNoHmrcAsAgentAuthAction
import uk.gov.hmrc.agentoverseasapplicationfrontend.forms.{AmlsDetailsForm, ContactDetailsForm, TradingAddressForm, TradingNameForm}
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.AgentSession
import uk.gov.hmrc.agentoverseasapplicationfrontend.services.SessionStoreService
import uk.gov.hmrc.agentoverseasapplicationfrontend.utils.{CountryNamesLoader, toFuture}
import uk.gov.hmrc.agentoverseasapplicationfrontend.views.html.{anti_money_laundering, contact_details, trading_address, trading_name}
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
      val form = session.contactDetails.fold(ContactDetailsForm.form)(ContactDetailsForm.form.fill)
      Ok(contact_details(form))
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
      val form = session.tradingName.fold(TradingNameForm.form)(TradingNameForm.form.fill)
      Ok(trading_name(form))
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

  def showTradingAddressForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      val form = TradingAddressForm.tradingAddressForm(validCountryCodes)
      Ok(trading_address(session.tradingAddress.fold(form)(form.fill), countries))
    }
  }

  def submitTradingAddress: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      TradingAddressForm
        .tradingAddressForm(validCountryCodes)
        .bindFromRequest()
        .fold(
          formWithErrors => Ok(trading_address(formWithErrors, countries)),
          validForm => updateSessionAndRedirect(session.copy(tradingAddress = Some(validForm)))
        )
    }
  }

  def showRegisteredWithHmrcForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    Ok("Success")
  }

  private def withAgentSession(body: AgentSession => Future[Result])(implicit hc: HeaderCarrier): Future[Result] =
    sessionStoreService.fetchAgentSession.flatMap {
      case Some(session) => body(session)
      case None          => Redirect(routes.ApplicationController.showAntiMoneyLaunderingForm())
    }

  private def updateSessionAndRedirect(agentSession: AgentSession)(implicit hc: HeaderCarrier): Future[Result] =
    sessionStoreService.cacheAgentSession(agentSession).flatMap(_ => lookupNextPage.map(Redirect))

}
