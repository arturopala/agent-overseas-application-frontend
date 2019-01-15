package uk.gov.hmrc.agentoverseasapplicationfrontend.services

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentoverseasapplicationfrontend.connectors.AgentOverseasApplicationConnector
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.{AgentSession, CreateApplicationRequest}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationService @Inject()(agentOverseasApplicationConnector: AgentOverseasApplicationConnector) {

  def createApplication(application: AgentSession)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    agentOverseasApplicationConnector.createOverseasApplication(CreateApplicationRequest(application.sanitize))
}
