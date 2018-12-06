package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers

import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.auth.{AgentAffinityNoHmrcAsAgentAuthAction, BasicAuthAction}
import uk.gov.hmrc.agentoverseasapplicationfrontend.services.SessionStoreService
import uk.gov.hmrc.agentoverseasapplicationfrontend.views.html.not_agent
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class StartController @Inject()(
  override val messagesApi: MessagesApi,
  val authConnector: AuthConnector,
  val env: Environment,
  basicAuthAction: BasicAuthAction,
  validApplicantAction: AgentAffinityNoHmrcAsAgentAuthAction,
  sessionStoreService: SessionStoreService)(implicit val configuration: Configuration, ec: ExecutionContext)
    extends FrontendController with I18nSupport {

  def root: Action[AnyContent] = validApplicantAction.async { implicit request =>
    Future.successful(Redirect(routes.ApplicationController.showAntiMoneyLaunderingForm()))
  }

  def showNotAgent: Action[AnyContent] = basicAuthAction.async { implicit request =>
    Future.successful(Ok(not_agent()))
  }

}
