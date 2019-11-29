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

package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers

import javax.inject.{Inject, Named, Singleton}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import play.api.{Configuration, Environment}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentoverseasapplicationfrontend.config.{AppConfig, CountryNamesLoader}
import uk.gov.hmrc.agentoverseasapplicationfrontend.connectors.UpscanConnector
import uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.auth.{AgentAffinityNoHmrcAsAgentAuthAction, BasicAgentAuthAction}
import uk.gov.hmrc.agentoverseasapplicationfrontend.services.{ApplicationService, SessionStoreService}
import uk.gov.hmrc.agentoverseasapplicationfrontend.views.html._

import scala.concurrent.ExecutionContext

@Singleton class AccessibilityStatementController @Inject()(
  val env: Environment,
  sessionStoreService: SessionStoreService,
  applicationService: ApplicationService,
  val upscanConnector: UpscanConnector,
  countryNamesLoader: CountryNamesLoader,
  validApplicantAction: AgentAffinityNoHmrcAsAgentAuthAction,
  appConfig: AppConfig)(
  implicit configuration: Configuration,
  messagesApi: MessagesApi,
  override val ec: ExecutionContext)
    extends AgentOverseasBaseController(sessionStoreService, applicationService) with SessionBehaviour
    with I18nSupport {

  def showAccessibilityStatement: Action[AnyContent] = Action { implicit request =>
    val userAction: String = request.headers.get(HeaderNames.REFERER).getOrElse("")
    val accessibilityUrl: String = s"${appConfig.accessibilityUrl}$userAction"
    Ok(accessibility_statement(accessibilityUrl))
  }
}
