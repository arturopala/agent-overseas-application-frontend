package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.auth

import javax.inject.Inject
import com.google.inject.ImplementedBy
import play.api.mvc.Results.{Forbidden, Redirect}
import play.api.mvc.{ActionBuilder, ActionFunction, Request, Result}
import play.api.{Configuration, Environment, Mode}
import uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.routes
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.CredentialRequest
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.Retrievals.{authorisedEnrolments, credentials}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects

import scala.concurrent.{ExecutionContext, Future}

class AgentAffinityNoEnrolmentAuthActionImpl @Inject()(
  val env: Environment,
  val authConnector: AuthConnector,
  val config: Configuration)(implicit ec: ExecutionContext)
    extends AgentAffinityNoHmrcAsAgentAuthAction with AuthorisedFunctions with AuthRedirects {

  def invokeBlock[A](request: Request[A], block: (CredentialRequest[A]) => Future[Result]): Future[Result] = {
    implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    authorised(AuthProviders(GovernmentGateway) and AffinityGroup.Agent)
      .retrieve(credentials and authorisedEnrolments) {
        case creds ~ enrolments =>
          if (!isEnrolledForHmrcAsAgent(enrolments)) block(CredentialRequest(creds.providerId, request))
          else Future.successful(Forbidden)
      }
      .recover {
        case _: NoActiveSession =>
          val isDevEnv =
            if (env.mode.equals(Mode.Test)) false else config.getString("run.mode").forall(Mode.Dev.toString.equals)
          toGGLogin(if (isDevEnv) s"http://${request.host}${request.uri}" else s"${request.uri}")
        case _: UnsupportedAffinityGroup =>
          Redirect(routes.StartController.showNotAgent)
      }
  }

  private def isEnrolledForHmrcAsAgent(enrolments: Enrolments): Boolean =
    enrolments.enrolments.find(_.key equals "HMRC-AS-AGENT").exists(_.isActivated)
}

@ImplementedBy(classOf[AgentAffinityNoEnrolmentAuthActionImpl])
trait AgentAffinityNoHmrcAsAgentAuthAction
    extends ActionBuilder[CredentialRequest] with ActionFunction[Request, CredentialRequest]
