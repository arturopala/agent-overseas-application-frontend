package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers
import play.api.mvc.{Result, Results}
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.AgentSession
import uk.gov.hmrc.agentoverseasapplicationfrontend.services.SessionStoreService
import uk.gov.hmrc.agentoverseasapplicationfrontend.utils.toFuture
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait SessionBehaviour extends CommonRouting with Results {
  val sessionStoreService: SessionStoreService
  implicit val ec: ExecutionContext

  def withAgentSession(body: AgentSession => Future[Result])(implicit hc: HeaderCarrier): Future[Result] =
    sessionStoreService.fetchAgentSession.flatMap {
      case Some(session) => body(session)
      case None          => Redirect(routes.ApplicationController.showAntiMoneyLaunderingForm())
    }

  def updateSessionAndRedirect(agentSession: AgentSession, redirectTo: Option[String] = None)(
    implicit hc: HeaderCarrier): Future[Result] =
    sessionStoreService
      .cacheAgentSession(agentSession)
      .flatMap(_ => redirectTo.fold(lookupNextPage.map(Redirect))(Redirect(_)))

}
