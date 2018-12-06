package uk.gov.hmrc.agentoverseasapplicationfrontend.services

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.{AgentSession, ContactDetails}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.SessionCache

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SessionStoreService @Inject()(val sessionCache: SessionCache) {

  def fetchAgentSession(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[AgentSession]] =
    sessionCache.fetchAndGetEntry[AgentSession]("agentSession")

  def cacheAgentSession(agentSession: AgentSession)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    sessionCache.cache("agentSession", agentSession).map(_ => ())

  def remove()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    sessionCache.remove().map(_ => ())
}
