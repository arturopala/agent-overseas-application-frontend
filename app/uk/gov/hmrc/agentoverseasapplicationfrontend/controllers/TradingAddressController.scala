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
import play.api.{Configuration, Environment}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.agentoverseasapplicationfrontend.config.CountryNamesLoader
import uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.auth.AgentAffinityNoHmrcAsAgentAuthAction
import uk.gov.hmrc.agentoverseasapplicationfrontend.forms.MainBusinessAddressForm
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.AgentSession
import uk.gov.hmrc.agentoverseasapplicationfrontend.services.{ApplicationService, SessionStoreService}
import uk.gov.hmrc.agentoverseasapplicationfrontend.views.html.main_business_address
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import uk.gov.hmrc.agentoverseasapplicationfrontend.utils.toFuture
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

@Singleton
class TradingAddressController @Inject()(
  override val messagesApi: MessagesApi,
  val env: Environment,
  val sessionStoreService: SessionStoreService,
  val applicationService: ApplicationService,
  countryNamesLoader: CountryNamesLoader,
  validApplicantAction: AgentAffinityNoHmrcAsAgentAuthAction)(
  implicit val configuration: Configuration,
  override val ec: ExecutionContext)
    extends FrontendController with SessionBehaviour with I18nSupport {

  private val countries = countryNamesLoader.load
  private val validCountryCodes = countries.keys.toSet

  private val showCheckYourAnswersUrl = routes.ApplicationController.showCheckYourAnswers().url

  def showMainBusinessAddressForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    val form = MainBusinessAddressForm.mainBusinessAddressForm(validCountryCodes)
    if (request.agentSession.changingAnswers) {
      Ok(
        main_business_address(
          request.agentSession.mainBusinessAddress.fold(form)(form.fill),
          countries,
          Some(showCheckYourAnswersUrl)))
    } else {
      Ok(main_business_address(request.agentSession.mainBusinessAddress.fold(form)(form.fill), countries))
    }
  }

  def submitMainBusinessAddress: Action[AnyContent] = validApplicantAction.async { implicit request =>
    MainBusinessAddressForm
      .mainBusinessAddressForm(validCountryCodes)
      .bindFromRequest()
      .fold(
        formWithErrors => {
          if (request.agentSession.changingAnswers) {
            Ok(main_business_address(formWithErrors, countries, Some(showCheckYourAnswersUrl)))
          } else {
            Ok(main_business_address(formWithErrors, countries))
          }
        },
        validForm =>
          updateSession(request.agentSession.copy(mainBusinessAddress = Some(validForm)))(
            routes.FileUploadController.showTradingAddressUploadForm().url)
      )
  }

  private def updateSession(agentSession: AgentSession)(redirectTo: String)(implicit hc: HeaderCarrier) =
    if (agentSession.changingAnswers) {
      updateSessionAndRedirect(agentSession.copy(changingAnswers = false))(showCheckYourAnswersUrl)
    } else {
      updateSessionAndRedirect(agentSession)(redirectTo)
    }
}
