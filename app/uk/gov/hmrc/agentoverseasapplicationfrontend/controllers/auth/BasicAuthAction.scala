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

package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.auth

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import play.api.mvc.{ActionBuilder, Request, Result}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BasicAuthActionImpl @Inject()(val env: Environment, val authConnector: AuthConnector, val config: Configuration)(
  implicit ec: ExecutionContext)
    extends BasicAuthAction with AuthAction {

  def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    authorised(AuthProviders(GovernmentGateway)) {
      block(request)
    }.recover(handleFailure(request))
  }
}

@ImplementedBy(classOf[BasicAuthActionImpl])
trait BasicAuthAction extends ActionBuilder[Request]
