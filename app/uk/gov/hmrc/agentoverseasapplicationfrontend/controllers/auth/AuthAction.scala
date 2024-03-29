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

import play.api.{Logger, Mode}
import play.api.mvc.{Request, Result}
import play.api.mvc.Results.{Forbidden, Redirect}
import uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.routes
import uk.gov.hmrc.auth.core.{AuthorisedFunctions, InsufficientEnrolments, NoActiveSession, UnsupportedAffinityGroup, UnsupportedAuthProvider}
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects

trait AuthAction extends AuthRedirects with AuthorisedFunctions {

  lazy val isDevEnv: Boolean =
    if (env.mode.equals(Mode.Test)) false else config.getString("run.mode").forall(Mode.Dev.toString.equals)

  protected def handleFailure(implicit request: Request[_]): PartialFunction[Throwable, Result] = {
    case _: NoActiveSession ⇒
      toGGLogin(if (isDevEnv) s"http://${request.host}${request.uri}" else s"${request.uri}")

    case _: InsufficientEnrolments ⇒
      Logger.warn(s"Logged in user does not have required enrolments")
      Forbidden

    case _: UnsupportedAuthProvider ⇒
      Logger.warn(s"user logged in with unsupported auth provider")
      Forbidden

    case _: UnsupportedAffinityGroup =>
      Logger.warn(s"user logged in with unsupported affinity group")
      Redirect(routes.StartController.showNotAgent())
  }

}
