package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers

import javax.inject.{Inject, Singleton}

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.auth.AgentAffinityNoHmrcAsAgentAuthAction
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

@Singleton
class ApplicationController @Inject()(
  override val messagesApi: MessagesApi,
  val authConnector: DefaultAuthConnector,
  val env: Environment,
  isValidApplicant: AgentAffinityNoHmrcAsAgentAuthAction)(implicit val configuration: Configuration)
    extends FrontendController with I18nSupport {

  def root: Action[AnyContent] = isValidApplicant.async { implicit request =>
    ???
  }
}
