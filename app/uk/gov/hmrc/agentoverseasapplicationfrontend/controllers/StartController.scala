package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers

import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.auth.{AgentAffinityNoHmrcAsAgentAuthAction, BasicAuthAction}
import uk.gov.hmrc.agentoverseasapplicationfrontend.services.{ApplicationService, SessionStoreService}
import uk.gov.hmrc.agentoverseasapplicationfrontend.views.html.{not_agent, status_rejected}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

class StartController @Inject()(
  override val messagesApi: MessagesApi,
  val authConnector: AuthConnector,
  val env: Environment,
  basicAuthAction: BasicAuthAction,
  validApplicantAction: AgentAffinityNoHmrcAsAgentAuthAction,
  sessionStoreService: SessionStoreService,
  applicationService: ApplicationService)(implicit val configuration: Configuration, ec: ExecutionContext)
    extends FrontendController with I18nSupport {

  def root: Action[AnyContent] = validApplicantAction.async { implicit request =>
    applicationService.rejectedApplication
      .map(_.fold(Redirect(routes.ApplicationController.showAntiMoneyLaunderingForm()))(rejectedApp =>
        Ok(status_rejected(rejectedApp))))
  }

  def showNotAgent: Action[AnyContent] = basicAuthAction.async { implicit request =>
    Future.successful(Ok(not_agent()))
  }

}
