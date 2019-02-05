package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{Clock, LocalDate, ZoneOffset}

import javax.inject.{Inject, Named}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.auth.{AgentAffinityNoHmrcAsAgentAuthAction, BasicAgentAuthAction, BasicAuthAction}
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.ApplicationStatus.{Pending, Rejected}
import uk.gov.hmrc.agentoverseasapplicationfrontend.services.{ApplicationService, SessionStoreService}
import uk.gov.hmrc.agentoverseasapplicationfrontend.views.html.{application_not_ready, not_agent, status_rejected}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.ExecutionContext

class StartController @Inject()(
  override val messagesApi: MessagesApi,
  val authConnector: AuthConnector,
  val env: Environment,
  basicAuthAction: BasicAuthAction,
  validApplicantAction: AgentAffinityNoHmrcAsAgentAuthAction,
  sessionStoreService: SessionStoreService,
  applicationService: ApplicationService,
  @Named("maintainer-application-review-days") daysToReviewApplication: Int,
  @Named("agent-overseas-subscription-frontend.root-path") subscriptionRootPath: String,
  basicAgentAuthAction: BasicAgentAuthAction)(implicit val configuration: Configuration, ec: ExecutionContext)
    extends FrontendController with I18nSupport {

  def root: Action[AnyContent] = basicAuthAction { implicit request =>
    Redirect(routes.ApplicationController.showAntiMoneyLaunderingForm())
  }

  def showNotAgent: Action[AnyContent] = basicAuthAction { implicit request =>
    Ok(not_agent())
  }

  def applicationStatus: Action[AnyContent] = basicAuthAction.async { implicit request =>
    applicationService.getCurrentApplication.map {
      case Some(application) if application.status == Pending => {
        val createdOnPrettifyDate: String = application.applicationCreationDate.format(
          DateTimeFormatter.ofPattern("d MMMM YYYY").withZone(ZoneOffset.UTC))
        val daysUntilReviewed: Int = daysUntilApplicationReviewed(application.applicationCreationDate)
        Ok(application_not_ready(application.tradingName, createdOnPrettifyDate, daysUntilReviewed))
      }
      case Some(application) if application.status == Rejected => Ok(status_rejected(application))
      case Some(_)                                             => SeeOther(subscriptionRootPath)
      case None                                                => Redirect(routes.StartController.root())
    }
  }

  private def daysUntilApplicationReviewed(applicationCreationDate: LocalDate): Int = {
    val daysUntilAppReviewed = LocalDate
      .now(Clock.systemUTC())
      .until(applicationCreationDate.plusDays(daysToReviewApplication), ChronoUnit.DAYS)
      .toInt
    if (daysUntilAppReviewed > 0) daysUntilAppReviewed else 0
  }
}
