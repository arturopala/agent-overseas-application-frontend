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

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.agentoverseasapplicationfrontend.connectors.UpscanConnector
import uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.auth.AgentAffinityNoHmrcAsAgentAuthAction
import uk.gov.hmrc.agentoverseasapplicationfrontend.forms.SuccessfulFileUploadForm
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.{TradingAddressUploadStatus, Yes, YesNo}
import uk.gov.hmrc.agentoverseasapplicationfrontend.services.{ApplicationService, SessionStoreService}
import uk.gov.hmrc.agentoverseasapplicationfrontend.utils.toFuture
import uk.gov.hmrc.agentoverseasapplicationfrontend.views.html._
import uk.gov.hmrc.auth.core.AuthConnector

import scala.concurrent.ExecutionContext

@Singleton
class FileUploadController @Inject()(
  authConnector: AuthConnector,
  sessionStoreService: SessionStoreService,
  validApplicantAction: AgentAffinityNoHmrcAsAgentAuthAction,
  applicationService: ApplicationService,
  upscanConnector: UpscanConnector)(
  implicit messagesApi: MessagesApi,
  ex: ExecutionContext,
  configuration: Configuration)
    extends AgentOverseasBaseController(authConnector, sessionStoreService, applicationService) with SessionBehaviour {

  def showTradingAddressUploadForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    sessionStoreService.fetchAgentSession.flatMap {
      case Some(agentSession) =>
        upscanConnector.initiate().flatMap { upscan =>
          sessionStoreService
            .cacheAgentSession(agentSession.copy(
              tradingAddressUploadStatus = Some(TradingAddressUploadStatus(reference = Some(upscan.reference)))))
            .map { _ =>
              Ok(trading_address_upload(upscan))
            }
        }
      case None => Redirect(routes.ApplicationController.showAntiMoneyLaunderingForm())
    }

  }

  def showTradingAddressNoJsCheckPage: Action[AnyContent] = validApplicantAction.async { implicit request =>
    Ok(trading_address_no_js_check_file())
  }

  def upscanPollStatus(reference: String) = validApplicantAction.async { implicit request =>
    sessionStoreService.fetchAgentSession.flatMap {
      case Some(agentSession) =>
        applicationService
          .upscanPollStatus(reference)
          .flatMap { response =>
            val tar = agentSession.tradingAddressUploadStatus.flatMap(_.reference)
            Some(response.reference) match {
              case tar => {
                sessionStoreService
                  .cacheAgentSession(agentSession.copy(tradingAddressUploadStatus =
                    agentSession.tradingAddressUploadStatus.map(_.copy(uploadStatus = Some(response)))))
                  .map { _ =>
                    Ok(Json.toJson(response))
                  }
              }

              case _ => throw new RuntimeException("expecting reference in the session but not found")
            }
          }
      case None => throw new RuntimeException("TODO")

    }
  }

  def showSuccessfulFileUploadedForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    Ok(successful_file_upload(SuccessfulFileUploadForm.form, "dummyFileName"))
  }

  def submitSuccessfulFileUploadedForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    SuccessfulFileUploadForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => Ok(successful_file_upload(formWithErrors, "dummyFileName")),
        validFormValue => {
          val newValue = YesNo(validFormValue)
          val redirectTo =
            if (Yes == newValue) routes.ApplicationController.showRegisteredWithHmrcForm().url
            else routes.FileUploadController.showTradingAddressUploadForm().url
          Redirect(redirectTo)
        }
      )
  }

  def showUploadFailedPage: Action[AnyContent] = validApplicantAction.async { implicit request =>
    sessionStoreService.fetchAgentSession.map {
      case Some(agentSession) =>
        agentSession.tradingAddressUploadStatus.flatMap(_.uploadStatus) match {
          case Some(uploadStatus) => Ok(file_upload_failed(uploadStatus))
          case None               => throw new RuntimeException("expecting uploadStatus in the session but not found")
        }
    }

  }
}
