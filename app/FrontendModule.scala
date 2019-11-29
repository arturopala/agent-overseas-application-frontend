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

import com.google.inject.AbstractModule
import javax.inject.{Inject, Singleton}
import org.slf4j.MDC
import play.api.Mode.Mode
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.agentoverseasapplicationfrontend.config.AppConfig
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.config.ServicesConfig

class FrontendModule(environment: Environment, configuration: Configuration) extends AbstractModule {

  def configure(): Unit = {

    val appName: String = "agent-overseas-application-frontend"

    val loggerDateFormat: Option[String] = configuration.getString("logger.json.dateformat")
    Logger(getClass).info(s"Starting microservice : $appName : in mode : ${environment.mode}")
    MDC.put("appName", appName)
    loggerDateFormat.foreach(str => MDC.put("logger.json.dateformat", str))

    bind(classOf[SessionCache]).to(classOf[ApplicationSessionCache])
    bind(classOf[ServicesConfig]).to(classOf[DefaultServicesConfig])
    ()
  }

}

private class DefaultServicesConfig @Inject()(environment: Environment, val runModeConfiguration: Configuration)
    extends ServicesConfig {
  override protected def mode: Mode = environment.mode
}

@Singleton
class ApplicationSessionCache @Inject()(val http: HttpClient, appConfig: AppConfig) extends SessionCache {
  override lazy val defaultSource = appConfig.appName
  override lazy val baseUri = appConfig.sessionCacheBaseUrl
  override def domain: String = appConfig.sessionCacheDomain
}
