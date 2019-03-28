package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers
import play.api.mvc.{Call, Result, Results}
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.AgentSession
import uk.gov.hmrc.agentoverseasapplicationfrontend.services.SessionStoreService
import uk.gov.hmrc.agentoverseasapplicationfrontend.utils.toFuture
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait SessionBehaviour extends CommonRouting with Results {
  val sessionStoreService: SessionStoreService
  implicit val ec: ExecutionContext

  def updateSessionAndRedirect(agentSession: AgentSession)(redirectTo: String)(
    implicit hc: HeaderCarrier): Future[Result] =
    sessionStoreService
      .cacheAgentSession(agentSession)
      .flatMap(_ => Redirect(redirectTo))
}
