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
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, RequestHeader}
import uk.gov.hmrc.agentoverseasapplicationfrontend.services.{ApplicationService, SessionStoreService}
import uk.gov.hmrc.agentoverseasapplicationfrontend.utils.toFuture
import uk.gov.hmrc.agentoverseasapplicationfrontend.views.html.error_template
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.ExecutionContext

@Singleton
class AgentOverseasBaseController @Inject()(
  val sessionStoreService: SessionStoreService,
  val applicationService: ApplicationService)(
  implicit val messagesApi: MessagesApi,
  val configuration: Configuration,
  val ec: ExecutionContext)
    extends BaseController with SessionBehaviour with I18nSupport {

  override implicit def hc(implicit rh: RequestHeader): HeaderCarrier =
    HeaderCarrierConverter.fromHeadersAndSessionAndRequest(rh.headers, Some(rh.session), Some(rh))



  def serverError: Action[AnyContent] = Action.async { implicit request =>
    Ok(error_template("global.error.500.title", "global.error.500.heading", "global.error.500.message"))
  }

}
