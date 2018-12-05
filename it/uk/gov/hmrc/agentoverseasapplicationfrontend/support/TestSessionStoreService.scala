package uk.gov.hmrc.agentoverseasapplicationfrontend.support

import uk.gov.hmrc.agentoverseasapplicationfrontend.models.AgentApplication
import uk.gov.hmrc.agentoverseasapplicationfrontend.services.SessionStoreService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class TestSessionStoreService extends SessionStoreService(null) {

  class Session(var agentApplication: Option[AgentApplication] = None)

  private val sessions = collection.mutable.Map[String, Session]()

  private def sessionKey(implicit hc: HeaderCarrier): String = hc.userId match {
    case None => "default"
    case Some(userId) => userId.toString
  }

  def currentSession(implicit hc: HeaderCarrier): Session =
    sessions.getOrElseUpdate(sessionKey, new Session())

  def clear(): Unit =
    sessions.clear()

  def allSessionsRemoved: Boolean =
    sessions.isEmpty

  override def fetchAgentApplication(
                                        implicit hc: HeaderCarrier,
                                        ec: ExecutionContext): Future[Option[AgentApplication]] =
    Future.successful(currentSession.agentApplication)

  override def cacheAgentApplication(
                                        agentApplication: AgentApplication)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    Future.successful(currentSession.agentApplication = Some(agentApplication))

  override def remove()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    Future {
      sessions.remove(sessionKey)
    }
}
