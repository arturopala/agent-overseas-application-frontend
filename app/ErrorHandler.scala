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

import com.google.inject.name.Named
import javax.inject.{Inject, Singleton}
import play.api.i18n.MessagesApi
import play.api.mvc.Results._
import play.api.mvc.{Request, RequestHeader, Result}
import play.api.{Configuration, Environment, Mode}
import uk.gov.hmrc.agentoverseasapplicationfrontend.config.AppConfig
import uk.gov.hmrc.agentoverseasapplicationfrontend.views.html.{error_template, error_template_5xx}
import uk.gov.hmrc.http.{JsValidationException, NotFoundException}
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.bootstrap.config.{AuthRedirects, HttpAuditEvent}
import uk.gov.hmrc.play.bootstrap.http.FrontendErrorHandler

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ErrorHandler @Inject()(
  val env: Environment,
  val messagesApi: MessagesApi,
  val auditConnector: AuditConnector,
  appConfig: AppConfig)(implicit val config: Configuration, ec: ExecutionContext)
    extends FrontendErrorHandler with AuthRedirects with ErrorAuditing {

  val appName: String = appConfig.appName

  private val isDevEnv =
    if (env.mode.equals(Mode.Test)) false else config.getString("run.mode").forall(Mode.Dev.toString.equals)

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    auditClientError(request, statusCode, message)
    super.onClientError(request, statusCode, message)
  }

  override def resolveError(request: RequestHeader, exception: Throwable) = {
    auditServerError(request, exception)
    implicit val r = Request(request, "")
    Ok(error_template_5xx())
  }

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(
    implicit request: Request[_]) =
    error_template(pageTitle, heading, message)
}

object EventTypes {

  val RequestReceived: String = "RequestReceived"
  val TransactionFailureReason: String = "transactionFailureReason"
  val ServerInternalError: String = "ServerInternalError"
  val ResourceNotFound: String = "ResourceNotFound"
  val ServerValidationError: String = "ServerValidationError"
  val UnknownError: String = "UnknownError"
}

trait ErrorAuditing extends HttpAuditEvent {

  import EventTypes._

  def auditConnector: AuditConnector

  private val unexpectedError = "Unexpected error"
  private val notFoundError = "Resource Endpoint Not Found"
  private val badRequestError = "Request bad format exception"

  def auditServerError(request: RequestHeader, ex: Throwable)(implicit ec: ExecutionContext): Future[AuditResult] = {
    val eventType = ex match {
      case _: NotFoundException     => ResourceNotFound
      case _: JsValidationException => ServerValidationError
      case _                        => ServerInternalError
    }
    val transactionName = ex match {
      case _: NotFoundException => notFoundError
      case _                    => unexpectedError
    }
    auditConnector.sendEvent(
      dataEvent(eventType, transactionName, request, Map(TransactionFailureReason -> ex.getMessage))(
        HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))))
  }

  def auditClientError(request: RequestHeader, statusCode: Int, message: String)(
    implicit ec: ExecutionContext): Future[AuditResult] = {
    import play.api.http.Status._
    statusCode match {
      case NOT_FOUND =>
        auditConnector.sendEvent(
          dataEvent(ResourceNotFound, notFoundError, request, Map(TransactionFailureReason -> message))(
            HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))))
      case BAD_REQUEST =>
        auditConnector.sendEvent(
          dataEvent(ServerValidationError, badRequestError, request, Map(TransactionFailureReason -> message))(
            HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))))
      case _ =>
        auditConnector.sendEvent(
          dataEvent(UnknownError, unexpectedError, request, Map(TransactionFailureReason -> message))(
            HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))))
    }
  }
}
