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

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import javax.inject.{Inject, Named, Singleton}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.upscan.UpscanInitiate
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, HttpPost}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpscanConnector @Inject()(
  @Named("upscan-baseUrl") upscanBaseUrl: URL,
  @Named("agent-overseas-application-baseUrl") overseasApplicationBaseUrl: URL,
  httpClient: HttpClient,
  metrics: Metrics)
    extends HttpAPIMonitor {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  val upscanUrl = new URL(upscanBaseUrl, "/upscan/initiate")

  val callBackUrl = new URL(overseasApplicationBaseUrl, "/agent-overseas-application/upscan-callback")

  val maxFileSize = 25000000 //25MB

  val request: JsValue = Json.parse(s"""{
                                       |"callbackUrl": "$callBackUrl",
                                       |"minimumFileSize": 1000,
                                       |"maximumFileSize": $maxFileSize
                                       |}
    """.stripMargin)

  def initiate()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UpscanInitiate] =
    monitor("upscan-initiate POST") {
      httpClient.POST[JsValue, JsValue](upscanUrl.toString, request, Seq("content-Type" -> "application/json")).map {
        response =>
          response.as[UpscanInitiate]
      }
    }
}
