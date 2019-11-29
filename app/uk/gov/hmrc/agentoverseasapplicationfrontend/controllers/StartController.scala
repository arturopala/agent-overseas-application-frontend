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

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{Clock, LocalDate, LocalDateTime, ZoneOffset}

import javax.inject.{Inject, Named}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.agentoverseasapplicationfrontend.config.AppConfig
import uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.auth.{AgentAffinityNoHmrcAsAgentAuthAction, BasicAgentAuthAction, BasicAuthAction}
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.ApplicationStatus.{Pending, Rejected}
import uk.gov.hmrc.agentoverseasapplicationfrontend.services.{ApplicationService, SessionStoreService}
import uk.gov.hmrc.agentoverseasapplicationfrontend.views.html.{application_not_ready, not_agent, status_rejected}

import scala.concurrent.ExecutionContext

class StartController @Inject()(
  val env: Environment,
  basicAuthAction: BasicAuthAction,
  validApplicantAction: AgentAffinityNoHmrcAsAgentAuthAction,
  sessionStoreService: SessionStoreService,
  applicationService: ApplicationService,
  appConfig: AppConfig,
  basicAgentAuthAction: BasicAgentAuthAction)(
  implicit configuration: Configuration,
  messagesApi: MessagesApi,
  ec: ExecutionContext)
    extends AgentOverseasBaseController(sessionStoreService, applicationService) with I18nSupport {

  def root: Action[AnyContent] = basicAuthAction { implicit request =>
    Redirect(routes.AntiMoneyLaunderingController.showMoneyLaunderingRequired())
  }

  def showNotAgent: Action[AnyContent] = basicAuthAction { implicit request =>
    Ok(not_agent())
  }

  def applicationStatus: Action[AnyContent] = basicAuthAction.async { implicit request =>
    applicationService.getCurrentApplication.map {
      case Some(application) if application.status == Pending =>
        val createdOnPrettifyDate: String = application.applicationCreationDate.format(
          DateTimeFormatter.ofPattern("d MMMM YYYY").withZone(ZoneOffset.UTC))
        val daysUntilReviewed: Int = daysUntilApplicationReviewed(application.applicationCreationDate)
        Ok(application_not_ready(application.tradingName, createdOnPrettifyDate, daysUntilReviewed))
      case Some(application) if application.status == Rejected => Ok(status_rejected(application))
      case Some(_)                                             => SeeOther(appConfig.agentOverseasSubscriptionFrontendRootPath)
      case None                                                => Redirect(routes.StartController.root())
    }
  }

  private def daysUntilApplicationReviewed(applicationCreationDate: LocalDateTime): Int = {
    val daysUntilAppReviewed = LocalDate
      .now(Clock.systemUTC())
      .until(applicationCreationDate.plusDays(appConfig.maintainerApplicationReviewDays), ChronoUnit.DAYS)
      .toInt
    if (daysUntilAppReviewed > 0) daysUntilAppReviewed else 0
  }
}
