package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers

import play.api.http.HttpVerbs.GET
import play.api.mvc.Call
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.AgentSession._
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.ApplicationStatus.{Accepted, AttemptingRegistration, Complete, Pending, Registered, Rejected}
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.{AgentSession, No, Yes}
import uk.gov.hmrc.agentoverseasapplicationfrontend.services.{ApplicationService, SessionStoreService}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait CommonRouting {
  case class StatusRouting(proceedTo: Call, initialiseAgentSession: Boolean)

  val sessionStoreService: SessionStoreService

  val applicationService: ApplicationService

  def lookupNextPage(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Call] =
    sessionStoreService.fetchAgentSession.map { session =>
      session match {
        case MissingAmlsDetails()        => routes.ApplicationController.showAntiMoneyLaunderingForm()
        case MissingContactDetails()     => routes.ApplicationController.showContactDetailsForm()
        case MissingTradingName()        => routes.ApplicationController.showTradingNameForm()
        case MissingTradingAddress()     => routes.ApplicationController.showMainBusinessAddressForm()
        case MissingRegisteredWithHmrc() => routes.ApplicationController.showRegisteredWithHmrcForm()
        case IsRegisteredWithHmrc(Yes)   => routesFromAgentCodesOnwards(session)
        case IsRegisteredWithHmrc(No)    => routesFromUkTaxRegistrationOnwards(session)
        case _                           => routes.ApplicationController.showAntiMoneyLaunderingForm()
      }
    }

  def routesIfExistingApplication(
    subscriptionRootPath: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Call] = {
    def routing: Future[StatusRouting] = applicationService.getCurrentApplication.map {
      case Some(application) if application.status == Rejected || application.status == Pending => {
        val initialiseSession = application.status == Rejected
        StatusRouting(routes.StartController.applicationStatus(), initialiseSession)
      }
      case Some(application)
          if Set(Accepted, AttemptingRegistration, Registered, Complete).contains(application.status) =>
        StatusRouting(Call(GET, subscriptionRootPath), false)
      case None => StatusRouting(routes.ApplicationController.showAntiMoneyLaunderingForm(), true)
    }

    for {
      proceed <- routing
      _ <- if (proceed.initialiseAgentSession) sessionStoreService.cacheAgentSession(AgentSession.empty)
          else Future.successful(())
    } yield proceed.proceedTo
  }

  private def routesFromAgentCodesOnwards(agentSession: Option[AgentSession]): Call = agentSession match {
    case MissingAgentCodes()                  => routes.ApplicationController.showAgentCodesForm()
    case HasAnsweredWithOneOrMoreAgentCodes() => routes.ApplicationController.showCheckYourAnswers()
    case HasAnsweredWithNoAgentCodes()        => routesFromUkTaxRegistrationOnwards(agentSession)
  }

  private def routesFromUkTaxRegistrationOnwards(agentSession: Option[AgentSession]): Call = agentSession match {
    case MissingRegisteredForUkTax() => routes.ApplicationController.showUkTaxRegistrationForm()
    case IsRegisteredForUkTax(Yes)   => showPersonalDetailsOrContinue(agentSession)
    case IsRegisteredForUkTax(No)    => collectCompanyRegNoOrContinue(agentSession)
  }

  private def showPersonalDetailsOrContinue(agentSession: Option[AgentSession]): Call = agentSession match {
    case MissingPersonalDetails() => routes.ApplicationController.showPersonalDetailsForm()
    case _                        => collectCompanyRegNoOrContinue(agentSession)
  }

  private def collectCompanyRegNoOrContinue(agentSession: Option[AgentSession]): Call = agentSession match {
    case MissingPersonalDetails()           => showPersonalDetailsOrContinue(agentSession)
    case MissingCompanyRegistrationNumber() => routes.ApplicationController.showCompanyRegistrationNumberForm()
    case _                                  => collectTaxRegNoOrContinue(agentSession)
  }

  private def collectTaxRegNoOrContinue(agentSession: Option[AgentSession]): Call = agentSession match {
    case MissingHasTaxRegistrationNumber() => routes.ApplicationController.showTaxRegistrationNumberForm()
    case HasTaxRegistrationNumber()        => routes.ApplicationController.showYourTaxRegNumbersForm()
    case NoTaxRegistrationNumber()         => routes.ApplicationController.showCheckYourAnswers()
  }
}
