package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers

import play.api.mvc.{Call, Results}
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.AgentSession._
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.{AgentSession, No, Unsure, Yes}
import uk.gov.hmrc.agentoverseasapplicationfrontend.services.SessionStoreService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait CommonRouting {
  this: Results =>

  val sessionStoreService: SessionStoreService

  def lookupNextPage(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Call] =
    sessionStoreService.fetchAgentSession.map { session =>
      session match {
        case MissingAmlsDetails()              => routes.ApplicationController.showAntiMoneyLaunderingForm()
        case MissingContactDetails()           => routes.ApplicationController.showContactDetailsForm()
        case MissingTradingName()              => routes.ApplicationController.showTradingNameForm()
        case MissingTradingAddress()           => routes.ApplicationController.showMainBusinessAddressForm()
        case MissingRegisteredWithHmrc()       => routes.ApplicationController.showRegisteredWithHmrcForm()
        case IsRegisteredWithHmrc(Yes)         => routesWhenRegisteredWithHmrc(session)
        case IsRegisteredWithHmrc(No | Unsure) => routesWhenNotRegisteredWithHmrc(session)
        case _                                 => routes.ApplicationController.showAntiMoneyLaunderingForm()
      }
    }

  private def routesWhenRegisteredWithHmrc(agentSession: Option[AgentSession]): Call =
    routes.ApplicationController.showSelfAssessmentAgentCodeForm()

  private def routesWhenNotRegisteredWithHmrc(agentSession: Option[AgentSession]): Call = agentSession match {
    case MissingRegisteredForUkTax()       => routes.ApplicationController.showUkTaxRegistrationForm()
    case IsRegisteredForUkTax(Yes)         => showPersonalDetailsOrContinue(agentSession)
    case IsRegisteredForUkTax(No | Unsure) => collectCompanyRegNoOrContinue(agentSession)
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
    case HasTaxRegistrationNumber()        => routes.ApplicationController.showYourTaxRegNo()
    case NoTaxRegistrationNumber()         => routes.ApplicationController.showCheckAnswers()
  }
}
