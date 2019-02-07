package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.auth

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Named, Singleton}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionBuilder, ActionFunction, Request, Result}
import play.api.{Configuration, Environment, Mode}
import uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.{CommonRouting, routes}
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.CredentialRequest
import uk.gov.hmrc.agentoverseasapplicationfrontend.services.{ApplicationService, SessionStoreService}
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.Retrievals.{allEnrolments, credentials}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AgentAffinityNoEnrolmentAuthActionImpl @Inject()(
  val env: Environment,
  val authConnector: AuthConnector,
  val config: Configuration,
  val applicationService: ApplicationService,
  val sessionStoreService: SessionStoreService,
  @Named("agent-services-account.root-path") agentServicesAccountRootPath: String,
  @Named("agent-overseas-subscription-frontend.root-path") subscriptionRootPath: String)(implicit ec: ExecutionContext)
    extends AgentAffinityNoHmrcAsAgentAuthAction with CommonRouting with AuthorisedFunctions with AuthRedirects {

  def invokeBlock[A](request: Request[A], block: CredentialRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    authorised(AuthProviders(GovernmentGateway) and AffinityGroup.Agent)
      .retrieve(credentials and allEnrolments) {
        case creds ~ enrolments =>
          if (isEnrolledForHmrcAsAgent(enrolments))
            Future.successful(Redirect(agentServicesAccountRootPath))
          else
            sessionStoreService.fetchAgentSession.flatMap {
              case Some(agentSession) => block(CredentialRequest(creds.providerId, request, agentSession))
              case None               => routesForApplicationStatuses(subscriptionRootPath).map(Redirect)
            }
      }
      .recover {
        case _: NoActiveSession =>
          val isDevEnv =
            if (env.mode.equals(Mode.Test)) false else config.getString("run.mode").forall(Mode.Dev.toString.equals)
          toGGLogin(if (isDevEnv) s"http://${request.host}${request.uri}" else s"${request.uri}")
        case _: UnsupportedAffinityGroup =>
          Redirect(routes.StartController.showNotAgent())
      }
  }

  private def isEnrolledForHmrcAsAgent(enrolments: Enrolments): Boolean =
    enrolments.enrolments.find(_.key equals "HMRC-AS-AGENT").exists(_.isActivated)
}

@ImplementedBy(classOf[AgentAffinityNoEnrolmentAuthActionImpl])
trait AgentAffinityNoHmrcAsAgentAuthAction
    extends ActionBuilder[CredentialRequest] with ActionFunction[Request, CredentialRequest]
