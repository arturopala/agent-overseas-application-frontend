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

import java.net.URL

import javax.inject.{Inject, Named, Singleton}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.agentoverseasapplicationfrontend.config.AppConfig
import uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.auth.BasicAuthAction
import uk.gov.hmrc.agentoverseasapplicationfrontend.utils.CallOps
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.ExecutionContext

@Singleton
class SignOutController @Inject()(
  override val messagesApi: MessagesApi,
  val env: Environment,
  basicAuthAction: BasicAuthAction,
  appConfig: AppConfig)(implicit val configuration: Configuration, ec: ExecutionContext)
    extends FrontendController with I18nSupport {

  def signOut: Action[AnyContent] = Action { implicit request =>
    SeeOther(appConfig.companyAuthSignInUrl).withNewSession
  }

  def signOutWithContinueUrl = Action { implicit request =>
    val continueUrl = routes.AntiMoneyLaunderingController.showAntiMoneyLaunderingForm().url
    SeeOther(CallOps.addParamsToUrl(appConfig.ggRegistrationFrontendSosRedirectPath, "continue" -> Some(continueUrl))).withNewSession
  }

  def startFeedbackSurvey: Action[AnyContent] = basicAuthAction { implicit request =>
    SeeOther(new URL(appConfig.feedbackSurveyUrl).toString).withNewSession
  }
}
