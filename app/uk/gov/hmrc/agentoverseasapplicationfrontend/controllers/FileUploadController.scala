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
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.auth.AgentAffinityNoHmrcAsAgentAuthAction
import uk.gov.hmrc.agentoverseasapplicationfrontend.forms.SuccessfulFileUploadForm
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.{Yes, YesNo}
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
  applicationService: ApplicationService)(
  implicit messagesApi: MessagesApi,
  ex: ExecutionContext,
  configuration: Configuration)
    extends AgentOverseasBaseController(authConnector, sessionStoreService, applicationService) {

  def showTradingAddressUploadForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    Ok(trading_address_upload())
  }

  def submitTradingAddressUploadForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    Redirect(routes.ApplicationController.showRegisteredWithHmrcForm().url)
  }

  def showTradingAddressUploadCheckingPage: Action[AnyContent] = validApplicantAction.async { implicit request =>
    Ok(trading_address_upload_in_progress())
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
    Ok(file_upload_failed())
  }
}
