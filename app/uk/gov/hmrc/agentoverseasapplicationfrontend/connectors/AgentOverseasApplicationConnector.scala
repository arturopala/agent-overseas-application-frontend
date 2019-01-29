package uk.gov.hmrc.agentoverseasapplicationfrontend.connectors

import java.net.URL
import java.time.LocalDate

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import javax.inject.{Inject, Named, Singleton}
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.ApplicationStatus.Rejected
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.{ApplicationEntityDetails, ApplicationStatus}
import uk.gov.hmrc.http._
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.CreateApplicationRequest
import uk.gov.hmrc.http.{HeaderCarrier, HttpPost, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AgentOverseasApplicationConnector @Inject()(
  @Named("agent-overseas-application-baseUrl") baseUrl: URL,
  http: HttpGet with HttpPost,
  metrics: Metrics
) extends HttpAPIMonitor {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  implicit val localDateOrdering: Ordering[LocalDate] = Ordering.by(_.toEpochDay)

  val allStatuses = ApplicationStatus.allStatuses.map(status => s"statusIdentifier=${status.key}").mkString("&")

  val urlGetAllApplications = new URL(baseUrl, s"/agent-overseas-application/application?$allStatuses")

  def getUserApplications(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[List[ApplicationEntityDetails]] =
    monitor(s"Agent-Overseas-Application-application-GET") {
      http
        .GET[List[ApplicationEntityDetails]](urlGetAllApplications.toString)
        .recover {
          case _: NotFoundException => List.empty
          case e                    => throw new RuntimeException(s"Could not retrieve overseas agent application status: ${e.getMessage}")
        }
    }

  def rejectedApplication(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ApplicationEntityDetails]] =
    monitor(s"Agent-Overseas-Application-application-GET") {
      http
        .GET[List[ApplicationEntityDetails]](urlGetAllApplications.toString)
        .map { apps =>
          if (apps.forall(_.status == Rejected))
            apps.sortBy(_.maintainerReviewedOn).reverse.headOption
          else None
        }
    }.recover {
      case _: NotFoundException => None
      case e                    => throw new RuntimeException(s"Could not retrieve overseas agent application status: ${e.getMessage}")
    }

  def createOverseasApplication(request: CreateApplicationRequest)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Unit] = {

    val url = new URL(baseUrl, s"/agent-overseas-application/application")
    monitor(s"Agent-Overseas-Application-application-POST") {
      http
        .POST[CreateApplicationRequest, HttpResponse](url.toString, request)
        .map(_ => ())
    }
  }
}
