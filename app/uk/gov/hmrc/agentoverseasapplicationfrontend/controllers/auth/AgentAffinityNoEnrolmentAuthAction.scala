/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.auth

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Named, Singleton}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionBuilder, ActionFunction, Request, Result}
import play.api.{Configuration, Environment, Logger, Mode}
import uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.{CommonRouting, routes}
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.CredentialRequest
import uk.gov.hmrc.agentoverseasapplicationfrontend.services.{ApplicationService, SessionStoreService}
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{allEnrolments, credentials}
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
        case credentialsOpt ~ enrolments =>
          if (isEnrolledForHmrcAsAgent(enrolments))
            Future.successful(Redirect(agentServicesAccountRootPath))
          else
            sessionStoreService.fetchAgentSession.flatMap {
              case Some(agentSession) =>
                credentialsOpt.fold(throw UnsupportedCredentialRole("User has no credentials"))(credentials =>
                  block(CredentialRequest(credentials.providerId, request, agentSession)))
              case None =>
                val root = subscriptionRootPath
                routesIfExistingApplication(subscriptionRootPath).map(Redirect)
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
