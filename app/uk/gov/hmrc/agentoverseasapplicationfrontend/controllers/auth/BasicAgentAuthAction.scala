package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.auth

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionBuilder, Request, Result}
import play.api.{Configuration, Environment, Mode}
import uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.routes
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BasicAgentAuthActionImpl @Inject()(
  val env: Environment,
  val authConnector: AuthConnector,
  val config: Configuration)(implicit ec: ExecutionContext)
    extends BasicAgentAuthAction with AuthorisedFunctions with AuthRedirects {

  def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    authorised(AuthProviders(GovernmentGateway) and AffinityGroup.Agent) {
      block(request)
    }.recover {
      case _: NoActiveSession =>
        val isDevEnv =
          if (env.mode.equals(Mode.Test)) false else config.getString("run.mode").forall(Mode.Dev.toString.equals)
        toGGLogin(if (isDevEnv) s"http://${request.host}${request.uri}" else s"${request.uri}")
      case _: UnsupportedAffinityGroup =>
        Redirect(routes.StartController.showNotAgent())
    }
  }
}

@ImplementedBy(classOf[BasicAgentAuthActionImpl])
trait BasicAgentAuthAction extends ActionBuilder[Request]