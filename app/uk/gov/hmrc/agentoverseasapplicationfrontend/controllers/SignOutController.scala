package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers

import javax.inject.{Inject, Named, Singleton}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.ExecutionContext

@Singleton
class SignOutController @Inject()(
  override val messagesApi: MessagesApi,
  val authConnector: AuthConnector,
  val env: Environment,
  @Named("companyAuthSignInUrl") val signInUrl: String)(implicit val configuration: Configuration, ec: ExecutionContext)
    extends FrontendController with I18nSupport {

  def signOut: Action[AnyContent] = Action { implicit request =>
    SeeOther(signInUrl).withNewSession
  }
}
