/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentoverseasapplicationfrontend.connectors

import java.net.URL
import java.time.{LocalDateTime, ZoneOffset}

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import javax.inject.{Inject, Named, Singleton}
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.{ApplicationEntityDetails, ApplicationStatus, CreateApplicationRequest, FileUploadStatus}
import uk.gov.hmrc.http.{HeaderCarrier, HttpPost, HttpResponse, _}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AgentOverseasApplicationConnector @Inject()(
  @Named("agent-overseas-application-baseUrl") baseUrl: URL,
  http: HttpGet with HttpPost,
  metrics: Metrics
) extends HttpAPIMonitor {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  implicit val localDateTimeOrdering: Ordering[LocalDateTime] = Ordering.by(_.toEpochSecond(ZoneOffset.UTC))

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

  def upscanPollStatus(
    reference: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[FileUploadStatus] = {
    val url = new URL(baseUrl, s"/agent-overseas-application/upscan-poll-status/$reference")
    monitor(s"Agent-overseas-Application-upscan-poll-status-GET") {
      http
        .GET[FileUploadStatus](url.toString)
    }
  }
}
