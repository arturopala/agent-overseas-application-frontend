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

package uk.gov.hmrc.agentoverseasapplicationfrontend.config

import com.google.inject.{ImplementedBy, Singleton}
import javax.inject.Inject
import play.api.Environment
import uk.gov.hmrc.play.config.ServicesConfig

@ImplementedBy(classOf[FrontendAppConfig])
trait AppConfig {
  val appName: String
  val countryListLocation: String
  val feedbackSurveyUrl: String
  val accessibilityUrl: String
  val maintainerApplicationReviewDays: Int
  val sessionCacheDomain: String
  val companyAuthSignInUrl: String
  val agentOverseasSubscriptionFrontendRootPath: String
  val ggRegistrationFrontendSosRedirectPath: String
  val agentServicesAccountPath: String
  val guidancePageApplicationUrl: String
  val authBaseUrl: String
  val sessionCacheBaseUrl: String
  val agentOverseasApplicationBaseUrl: String
  val upscanBaseUrl: String
}

@Singleton
class FrontendAppConfig @Inject()(servicesConfig: ServicesConfig, environment: Environment) extends AppConfig {
  override val appName: String = "agent-overseas-application-frontend"
  override val countryListLocation: String = servicesConfig.getString("country.list.location")
  override val feedbackSurveyUrl: String = servicesConfig.getString("feedback-survey-url")
  override val accessibilityUrl: String = servicesConfig.getString("accessibilityUrl")
  override val maintainerApplicationReviewDays: Int = servicesConfig.getInt("maintainer-application-review-days")
  override val sessionCacheDomain: String =
    servicesConfig.getString("microservice.services.cachable.session-cache.domain")
  override val companyAuthSignInUrl: String = servicesConfig.getString("microservice.services.companyAuthSignInUrl")
  override val agentOverseasSubscriptionFrontendRootPath: String =
    servicesConfig.getString("microservice.services.agent-overseas-subscription-frontend.root-path")
  override val ggRegistrationFrontendSosRedirectPath: String =
    servicesConfig.getString("microservice.services.government-gateway-registration-frontend.sosRedirect-path")
  override val agentServicesAccountPath: String =
    servicesConfig.getString("microservice.services.agent-services-account.root-path")
  override val guidancePageApplicationUrl: String =
    servicesConfig.getString("microservice.services.guidancePageApplicationUrl")
  override val authBaseUrl: String = servicesConfig.baseUrl("auth")
  override val sessionCacheBaseUrl: String = servicesConfig.baseUrl("cachable.session-cache")
  override val agentOverseasApplicationBaseUrl: String = servicesConfig.baseUrl("agent-overseas-application")
  override val upscanBaseUrl: String = servicesConfig.baseUrl("upscan")
}
