package uk.gov.hmrc.agentoverseasapplicationfrontend.services

import java.time.LocalDate

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentoverseasapplicationfrontend.connectors.AgentOverseasApplicationConnector
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.ApplicationEntityDetails
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.{AgentSession, CreateApplicationRequest}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationService @Inject()(agentOverseasApplicationConnector: AgentOverseasApplicationConnector) {

  implicit val orderingLocalDate: Ordering[LocalDate] = Ordering.by(d => (d.getYear, d.getDayOfYear))

  def getCurrentApplication(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext): Future[Option[ApplicationEntityDetails]] =
    agentOverseasApplicationConnector.getUserApplications.map { e =>
      e.sortBy(_.applicationCreationDate).reverse.headOption
    }

  def rejectedApplication(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ApplicationEntityDetails]] =
    agentOverseasApplicationConnector.rejectedApplication

  def createApplication(application: AgentSession)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    agentOverseasApplicationConnector.createOverseasApplication(CreateApplicationRequest(application.sanitize))
}
