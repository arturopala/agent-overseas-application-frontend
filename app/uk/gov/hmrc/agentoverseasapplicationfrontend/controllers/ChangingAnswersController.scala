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
import uk.gov.hmrc.agentoverseasapplicationfrontend.config.CountryNamesLoader
import uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.auth.AgentAffinityNoHmrcAsAgentAuthAction
import uk.gov.hmrc.agentoverseasapplicationfrontend.services.{ApplicationService, SessionStoreService}

import scala.concurrent.ExecutionContext

@Singleton
class ChangingAnswersController @Inject()(
  val env: Environment,
  validApplicantAction: AgentAffinityNoHmrcAsAgentAuthAction,
  override val sessionStoreService: SessionStoreService,
  override val applicationService: ApplicationService,
  countryNamesLoader: CountryNamesLoader)(
  implicit configuration: Configuration,
  messagesApi: MessagesApi,
  override val ec: ExecutionContext)
    extends AgentOverseasBaseController(sessionStoreService, applicationService) with SessionBehaviour
    with I18nSupport {

  def changeAmlsDetails: Action[AnyContent] = validApplicantAction.async { implicit request =>
    updateSessionAndRedirect(request.agentSession.copy(changingAnswers = true))(
      routes.AntiMoneyLaunderingController.showAntiMoneyLaunderingForm().url)
  }

  def changeAmlsFile: Action[AnyContent] = validApplicantAction.async { implicit request =>
    updateSessionAndRedirect(request.agentSession.copy(changingAnswers = true))(
      routes.FileUploadController.showAmlsUploadForm().url)
  }

  def changeContactDetails: Action[AnyContent] = validApplicantAction.async { implicit request =>
    updateSessionAndRedirect(request.agentSession.copy(changingAnswers = true))(
      routes.ApplicationController.showContactDetailsForm().url)
  }

  def changeTradingName: Action[AnyContent] = validApplicantAction.async { implicit request =>
    updateSessionAndRedirect(request.agentSession.copy(changingAnswers = true))(
      routes.ApplicationController.showTradingNameForm().url)
  }

  def changeTradingAddress: Action[AnyContent] = validApplicantAction.async { implicit request =>
    updateSessionAndRedirect(request.agentSession.copy(changingAnswers = true))(
      routes.TradingAddressController.showMainBusinessAddressForm().url)
  }

  def changeTradingAddressFile: Action[AnyContent] = validApplicantAction.async { implicit request =>
    updateSessionAndRedirect(request.agentSession.copy(changingAnswers = true))(
      routes.FileUploadController.showTradingAddressUploadForm().url)
  }

  def changeRegisteredWithHmrc: Action[AnyContent] = validApplicantAction.async { implicit request =>
    updateSessionAndRedirect(request.agentSession.copy(changingAnswers = true))(
      routes.ApplicationController.showRegisteredWithHmrcForm().url)
  }

  def changeAgentCodes: Action[AnyContent] = validApplicantAction.async { implicit request =>
    updateSessionAndRedirect(request.agentSession.copy(changingAnswers = true))(
      routes.ApplicationController.showAgentCodesForm().url)
  }

  def changeRegisteredForUKTax: Action[AnyContent] = validApplicantAction.async { implicit request =>
    updateSessionAndRedirect(request.agentSession.copy(changingAnswers = true))(
      routes.ApplicationController.showUkTaxRegistrationForm().url)
  }

  def changePersonalDetails: Action[AnyContent] = validApplicantAction.async { implicit request =>
    updateSessionAndRedirect(request.agentSession.copy(changingAnswers = true))(
      routes.ApplicationController.showPersonalDetailsForm().url)
  }

  def changeCompanyRegistrationNumber: Action[AnyContent] = validApplicantAction.async { implicit request =>
    updateSessionAndRedirect(request.agentSession.copy(changingAnswers = true))(
      routes.ApplicationController.showCompanyRegistrationNumberForm().url)
  }

  def changeYourTaxRegistrationNumbers: Action[AnyContent] = validApplicantAction.async { implicit request =>
    updateSessionAndRedirect(request.agentSession.copy(changingAnswers = true))(
      routes.ApplicationController.showYourTaxRegNumbersForm().url)
  }

  def changeYourTaxRegistrationNumbersFile: Action[AnyContent] = validApplicantAction.async { implicit request =>
    updateSessionAndRedirect(request.agentSession.copy(changingAnswers = true))(
      routes.FileUploadController.showTrnUploadForm().url)
  }
}
