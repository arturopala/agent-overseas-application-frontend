package uk.gov.hmrc.agentoverseasapplicationfrontend.connectors

import java.net.URL

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import javax.inject.{Inject, Named, Singleton}
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.CreateApplicationRequest
import uk.gov.hmrc.http.{HeaderCarrier, HttpPost, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AgentOverseasApplicationConnector @Inject()(
  @Named("agent-overseas-application-baseUrl") baseUrl: URL,
  http: HttpPost,
  metrics: Metrics)
    extends HttpAPIMonitor {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  def createOverseasApplication(
    request: CreateApplicationRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {

    val url = new URL(baseUrl, s"/agent-overseas-application/application")
    monitor(s"Agent-Overseas-Application-application-POST") {
      http
        .POST[CreateApplicationRequest, HttpResponse](url.toString, request)
        .map(_ => ())
    }
  }
}
