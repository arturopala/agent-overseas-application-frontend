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
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Result}
import play.api.{Configuration, Logger}
import uk.gov.hmrc.agentoverseasapplicationfrontend.connectors.UpscanConnector
import uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.auth.AgentAffinityNoHmrcAsAgentAuthAction
import uk.gov.hmrc.agentoverseasapplicationfrontend.forms.SuccessfulFileUploadConfirmationForm
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.{CredentialRequest, Yes, YesNo}
import uk.gov.hmrc.agentoverseasapplicationfrontend.services.{ApplicationService, SessionStoreService}
import uk.gov.hmrc.agentoverseasapplicationfrontend.utils.toFuture
import uk.gov.hmrc.agentoverseasapplicationfrontend.views.html._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

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

  private val showCheckYourAnswersUrl = routes.ApplicationController.showCheckYourAnswers().url

  def showUploadForm(fileType: String): Action[AnyContent] = validApplicantAction.async {
    implicit request: CredentialRequest[AnyContent] =>
      upscanConnector.initiate().map(upscan => Ok(file_upload(upscan, fileType, getBackLink(fileType))))
  }

  def showTradingAddressNoJsCheckPage: Action[AnyContent] = validApplicantAction.async { implicit request =>
    Ok(trading_address_no_js_check_file())
  }

  //these pollStatus functions are called via ajax in the assets/javascripts/script.js
  def pollStatus(fileType: String, reference: String) = validApplicantAction.async { implicit request =>
    upscanPollStatus(fileType, reference)
  }

  def showSuccessfulUploadedForm(fileType: String): Action[AnyContent] = validApplicantAction.async {
    implicit request =>
      getFileNameFromSession(fileType).map(
        filename =>
          Ok(
            successful_file_upload(
              SuccessfulFileUploadConfirmationForm.form,
              filename,
              fileType,
              Some(routes.FileUploadController.showUploadForm(fileType).url))))
  }

  def submitSuccessfulFileUploadedForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    SuccessfulFileUploadConfirmationForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          val fileType = formWithErrors.data("fileType")
          getFileNameFromSession(fileType).map(
            filename =>
              Ok(
                successful_file_upload(
                  formWithErrors,
                  filename,
                  fileType,
                  Some(routes.FileUploadController.showUploadForm(fileType).url))))
        },
        validForm => {
          val fileType = validForm.fileType
          val newValue = YesNo(validForm.choice)
          val redirectTo =
            if (Yes == newValue) {
              nextPage(fileType)
            } else routes.FileUploadController.showUploadForm(fileType).url
          Redirect(redirectTo)
        }
      )
  }

  def showUploadFailedPage(fileType: String): Action[AnyContent] = validApplicantAction.async { implicit request =>
    sessionStoreService.fetchAgentSession.map {
      case Some(agentSession) =>
        agentSession.tradingAddressUploadStatus match {
          case Some(uploadStatus) =>
            Ok(
              file_upload_failed(
                uploadStatus,
                fileType,
                Some(routes.FileUploadController.showUploadForm(fileType).url)))
          case None => throw new RuntimeException("expecting uploadStatus in the session but not found")
        }
    }
  }

  private def upscanPollStatus(fileType: String, reference: String)(implicit hc: HeaderCarrier): Future[Result] =
    sessionStoreService.fetchAgentSession.flatMap {
      case Some(agentSession) =>
        applicationService
          .upscanPollStatus(reference)
          .flatMap { response =>
            {
              val updatedSession = fileType match {
                case "trading-address" =>
                  agentSession.copy(tradingAddressUploadStatus = Some(response))
                case "amls" =>
                  agentSession.copy(amlsUploadStatus = Some(response))
                case "trn" =>
                  agentSession.copy(trnUploadStatus = Some(response))
                case _ => throw new RuntimeException("unexpected file status returned from aws")
              }
              sessionStoreService.cacheAgentSession(updatedSession).flatMap(_ => Ok(Json.toJson(response)))
            }
          }
      case None => throw new RuntimeException("TODO")
    }

  private def getFileNameFromSession(fileType: String)(implicit hc: HeaderCarrier): Future[Option[String]] =
    sessionStoreService.fetchAgentSession.flatMap {
      case Some(agentSession) => {
        fileType match {
          case "trading-address" => agentSession.tradingAddressUploadStatus
          case "amls"            => agentSession.amlsUploadStatus
          case "trn"             => agentSession.trnUploadStatus
          case _                 => throw new RuntimeException("could not get filename from session - unexpected fileType identifier")
        }

      }.map(_.fileName)
        .getOrElse(throw new RuntimeException("could not get filename from session - no file name in session"))
      case None => throw new RuntimeException("no agent session")
    }

  private def getBackLink(fileType: String)(implicit request: CredentialRequest[AnyContent]): Option[String] =
    if (request.agentSession.changingAnswers) {
      Some(showCheckYourAnswersUrl)
    } else {
      fileType match {
        case "trading-address" => Some(routes.TradingAddressController.showMainBusinessAddressForm().url)
        case "amls"            => Some(routes.AntiMoneyLaunderingController.showAntiMoneyLaunderingForm().url)
        case "trn"             => Some(routes.ApplicationController.showYourTaxRegNumbersForm().url)
        case _ => {
          Logger.info("routing error for back link- unrecognized document proof file key!")
          None
        }
      }
    }

  private def nextPage(fileType: String)(implicit request: CredentialRequest[AnyContent]): String =
    if (request.agentSession.changingAnswers) {
      showCheckYourAnswersUrl
    } else {
      fileType match {
        case "trading-address" => routes.ApplicationController.showRegisteredWithHmrcForm().url
        case "amls"            => routes.ApplicationController.showContactDetailsForm().url
        case "trn"             => routes.ApplicationController.showCheckYourAnswers().url
        case _                 => throw new RuntimeException("routing error for next page- unrecognized document proof file key!")
      }
    }

}
