package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers

import javax.inject.{Inject, Singleton}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, Result}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.auth.AgentAffinityNoHmrcAsAgentAuthAction
import uk.gov.hmrc.agentoverseasapplicationfrontend.forms.{AmlsDetailsForm, ContactDetailsForm, TradingNameForm}
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.AgentSession
import uk.gov.hmrc.agentoverseasapplicationfrontend.services.SessionStoreService
import uk.gov.hmrc.agentoverseasapplicationfrontend.utils.toFuture
import uk.gov.hmrc.agentoverseasapplicationfrontend.views.html.{anti_money_laundering, contact_details, trading_name}
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
  val sessionStoreService: SessionStoreService)(implicit val configuration: Configuration, ec: ExecutionContext)
    extends FrontendController with I18nSupport with CommonRouting {

  def showAntiMoneyLaunderingForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    sessionStoreService.fetchAgentSession.map {
      case Some(AgentSession(Some(amlsDetails), _, _, _)) =>
        Ok(anti_money_laundering(AmlsDetailsForm.form.fill(amlsDetails)))
      case _ => Ok(anti_money_laundering(AmlsDetailsForm.form))
    }
  }

  def submitAntiMoneyLaundering: Action[AnyContent] = validApplicantAction.async { implicit request =>
    AmlsDetailsForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => Ok(anti_money_laundering(formWithErrors)),
        validForm => {
          val call = routes.ApplicationController.showContactDetailsForm()

          sessionStoreService.fetchAgentSession.flatMap {
            case Some(session) => updateSessionAndRedirect(session.copy(amlsDetails = Some(validForm)))(call)
            case None          => updateSessionAndRedirect(AgentSession(Some(validForm)))(call)
          }
        }
      )
  }

  def showContactDetailsForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      val form =
        if (session.contactDetails.nonEmpty)
          ContactDetailsForm.form.fill(session.contactDetails.get)
        else ContactDetailsForm.form

      Ok(contact_details(form))
    }
  }

  def submitContactDetails: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      ContactDetailsForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => Ok(contact_details(formWithErrors)),
          validForm => redirect(session.copy(contactDetails = Some(validForm)))
        )
    }
  }

  def showTradingNameForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      val form =
        if (session.tradingName.nonEmpty)
          TradingNameForm.form.fill(session.tradingName.get)
        else TradingNameForm.form

      Ok(trading_name(form))
    }
  }

  def submitTradingName: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      TradingNameForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => Ok(trading_name(formWithErrors)),
          validForm => redirect(session.copy(tradingName = Some(validForm)))
        )
    }
  }

  def showMainBusinessAddressForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    Ok
  }

  private def withAgentSession(body: AgentSession => Future[Result])(implicit hc: HeaderCarrier): Future[Result] =
    sessionStoreService.fetchAgentSession.flatMap {
      case Some(session) => body(session)
      case None          => Redirect(routes.ApplicationController.showAntiMoneyLaunderingForm())
    }

  private def redirect(agentSession: AgentSession)(implicit hc: HeaderCarrier): Future[Result] =
    sessionStoreService.cacheAgentSession(agentSession).flatMap(_ => lookupNextPage.map(Redirect))

  private def updateSessionAndRedirect(agentSession: AgentSession)(redirect: => Call)(implicit hc: HeaderCarrier) =
    sessionStoreService
      .cacheAgentSession(agentSession)
      .map(_ => Redirect(redirect))
}
