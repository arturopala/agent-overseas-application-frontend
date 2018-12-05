package uk.gov.hmrc.agentoverseasapplicationfrontend.services

import javax.inject.{Inject, Singleton}

import uk.gov.hmrc.agentoverseasapplicationfrontend.models.AgentApplication
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.SessionCache

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SessionStoreService @Inject()(sessionCache: SessionCache) {

  def fetchAgentApplication(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[AgentApplication]] =
    sessionCache.fetchAndGetEntry[AgentApplication]("agentApplication")

  def cacheAgentApplication(
    application: AgentApplication)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    sessionCache.cache("agentApplication", application).map(_ => ())

  def remove()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    sessionCache.remove().map(_ => ())
}
