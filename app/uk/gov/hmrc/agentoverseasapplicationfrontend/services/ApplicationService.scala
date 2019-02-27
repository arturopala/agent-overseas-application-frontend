package uk.gov.hmrc.agentoverseasapplicationfrontend.services

import java.time.{LocalDateTime, ZoneOffset}

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentoverseasapplicationfrontend.connectors.AgentOverseasApplicationConnector
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.ApplicationStatus.Rejected
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.{AgentSession, ApplicationEntityDetails, CreateApplicationRequest}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationService @Inject()(agentOverseasApplicationConnector: AgentOverseasApplicationConnector) {

  implicit val orderingLocalDateTime: Ordering[LocalDateTime] = Ordering.by(_.toEpochSecond(ZoneOffset.UTC))

  def getCurrentApplication(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext): Future[Option[ApplicationEntityDetails]] =
    agentOverseasApplicationConnector.getUserApplications.map { e =>
      e.sortBy(_.applicationCreationDate).reverse.headOption
    }

  def rejectedApplication(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ApplicationEntityDetails]] =
    agentOverseasApplicationConnector.getUserApplications
      .map { apps =>
        if (apps.forall(_.status == Rejected))
          apps.sortBy(_.maintainerReviewedOn).reverse.headOption
        else None
      }

  def createApplication(application: AgentSession)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    agentOverseasApplicationConnector.createOverseasApplication(CreateApplicationRequest(application.sanitize))
}
