package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers

import javax.inject.{Inject, Singleton}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call}
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.auth.AgentAffinityNoHmrcAsAgentAuthAction
import uk.gov.hmrc.agentoverseasapplicationfrontend.forms.{AmlsDetailsForm, ContactDetailsForm}
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.AgentSession
import uk.gov.hmrc.agentoverseasapplicationfrontend.services.SessionStoreService
import uk.gov.hmrc.agentoverseasapplicationfrontend.utils.toFuture
import uk.gov.hmrc.agentoverseasapplicationfrontend.views.html.{anti_money_laundering, contact_details}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.ExecutionContext

@Singleton
class ApplicationController @Inject()(
  override val messagesApi: MessagesApi,
  val authConnector: AuthConnector,
  val env: Environment,
  validApplicantAction: AgentAffinityNoHmrcAsAgentAuthAction,
  sessionStoreService: SessionStoreService)(implicit val configuration: Configuration, ec: ExecutionContext)
    extends FrontendController with I18nSupport {

  def showAntiMoneyLaunderingForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    sessionStoreService.fetchAgentSession.map {
      case Some(AgentSession(Some(amlsDetails), _)) =>
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
    sessionStoreService.fetchAgentSession.map {
      case Some(AgentSession(Some(_), Some(contactDetails))) =>
        Ok(contact_details(ContactDetailsForm.form.fill(contactDetails)))
      case Some(AgentSession(Some(_), None)) =>
        Ok(contact_details(ContactDetailsForm.form))
      case _ => Redirect(routes.ApplicationController.showAntiMoneyLaunderingForm())
    }
  }

  def submitContactDetails: Action[AnyContent] = validApplicantAction.async { implicit request =>
    ContactDetailsForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => Ok(contact_details(formWithErrors)),
        validForm => {
          val call = routes.ApplicationController.showTradingNameForm()
          sessionStoreService.fetchAgentSession.flatMap {
            case Some(session) => updateSessionAndRedirect(session.copy(contactDetails = Some(validForm)))(call)
            case None          =>
              //should not happen as GET /contact-details makes sure that a valid session exists
              Logger.warn("no agent session found during storing contact details")
              InternalServerError
          }
        }
      )
  }

  def showTradingNameForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    Ok("Success")
  }

  private def updateSessionAndRedirect(agentSession: AgentSession)(redirect: => Call)(implicit hc: HeaderCarrier) =
    sessionStoreService
      .cacheAgentSession(agentSession)
      .map(_ => Redirect(redirect))
}
