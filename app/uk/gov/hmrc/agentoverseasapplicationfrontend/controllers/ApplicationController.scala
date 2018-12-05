package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers

import javax.inject.{Inject, Singleton}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.auth.AgentAffinityNoHmrcAsAgentAuthAction
import uk.gov.hmrc.agentoverseasapplicationfrontend.forms.AmlsDetailsForm
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.AgentApplication
import uk.gov.hmrc.agentoverseasapplicationfrontend.services.SessionStoreService
import uk.gov.hmrc.agentoverseasapplicationfrontend.views.html.anti_money_laundering
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationController @Inject()(
  override val messagesApi: MessagesApi,
  val authConnector: DefaultAuthConnector,
  val env: Environment,
  validApplicantAction: AgentAffinityNoHmrcAsAgentAuthAction,
  sessionStoreService: SessionStoreService)(implicit val configuration: Configuration, ec: ExecutionContext)
    extends FrontendController with I18nSupport {

  def showAntiMoneyLaunderingForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    Future.successful(Ok(anti_money_laundering(AmlsDetailsForm.form)))
  }

  def submitAntiMoneyLaundering: Action[AnyContent] = validApplicantAction.async { implicit request =>
    AmlsDetailsForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(Ok(anti_money_laundering(formWithErrors))),
        validForm =>
          sessionStoreService
            .cacheAgentApplication(AgentApplication(validForm))
            .map(_ => Redirect(routes.ApplicationController.showContactDetailsForm))
      )
  }

  def showContactDetailsForm: Action[AnyContent] = validApplicantAction { implicit request =>
    Ok
  }
}
