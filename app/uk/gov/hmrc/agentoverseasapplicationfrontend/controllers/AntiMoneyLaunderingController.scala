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
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.agentoverseasapplicationfrontend.connectors.UpscanConnector
import uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.auth.AgentAffinityNoHmrcAsAgentAuthAction
import uk.gov.hmrc.agentoverseasapplicationfrontend.forms.AmlsDetailsForm
import uk.gov.hmrc.agentoverseasapplicationfrontend.forms.YesNoRadioButtonForms.amlsRequiredForm
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.AgentSession
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.ApplicationStatus.Rejected
import uk.gov.hmrc.agentoverseasapplicationfrontend.services.{ApplicationService, SessionStoreService}
import uk.gov.hmrc.agentoverseasapplicationfrontend.utils.toFuture
import uk.gov.hmrc.agentoverseasapplicationfrontend.views.html.{anti_money_laundering, anti_money_laundering_required}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AntiMoneyLaunderingController @Inject()(
  val env: Environment,
  sessionStoreService: SessionStoreService,
  applicationService: ApplicationService,
  val upscanConnector: UpscanConnector,
  validApplicantAction: AgentAffinityNoHmrcAsAgentAuthAction)(
  implicit configuration: Configuration,
  override val messagesApi: MessagesApi,
  override val ec: ExecutionContext)
    extends AgentOverseasBaseController(sessionStoreService, applicationService) with SessionBehaviour
    with I18nSupport {

  def showMoneyLaunderingRequired: Action[AnyContent] = validApplicantAction.async { implicit request =>
    Ok(anti_money_laundering_required(amlsRequiredForm))
  }

  def submitMoneyLaunderingRequired: Action[AnyContent] = validApplicantAction.async { implicit request =>
    amlsRequiredForm
      .bindFromRequest()
      .fold(
        formWithErrors => {
          Ok(anti_money_laundering_required(formWithErrors))
        },
        isRequired =>
          for {
            sessionOpt <- sessionStoreService.fetchAgentSession
            session = sessionOpt.getOrElse(AgentSession())
            updatedSession = session.copy(amlsRequired = Some(isRequired.value))
            redirectUrl = if (isRequired.value) routes.AntiMoneyLaunderingController.showAntiMoneyLaunderingForm().url
            else routes.ApplicationController.showContactDetailsForm().url
            result <- updateSession(updatedSession)(redirectUrl)
          } yield result
      )
  }

  def showAntiMoneyLaunderingForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    val form = AmlsDetailsForm.form

    val backUrl: Future[Option[String]] = {
      if (request.agentSession.changingAnswers) Some(showCheckYourAnswersUrl)
      else
        applicationService.getCurrentApplication.map {
          case Some(application) if application.status == Rejected =>
            Some(routes.StartController.applicationStatus().url)
          case _ => Some(routes.AntiMoneyLaunderingController.showMoneyLaunderingRequired().url)
        }
    }

    backUrl.map(url => Ok(anti_money_laundering(request.agentSession.amlsDetails.fold(form)(form.fill), url)))
  }

  def submitAntiMoneyLaundering: Action[AnyContent] = validApplicantAction.async { implicit request =>
    AmlsDetailsForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          sessionStoreService.fetchAgentSession.map {
            case Some(session) if session.changingAnswers =>
              Ok(anti_money_laundering(formWithErrors, Some(showCheckYourAnswersUrl)))
            case _ =>
              Ok(anti_money_laundering(formWithErrors))
          }
        },
        validForm => {
          sessionStoreService.fetchAgentSession
            .map(_.getOrElse(AgentSession()))
            .map(_.copy(amlsDetails = Some(validForm)))
            .flatMap(updateSession(_)(routes.FileUploadController.showAmlsUploadForm().url))
        }
      )
  }
}
