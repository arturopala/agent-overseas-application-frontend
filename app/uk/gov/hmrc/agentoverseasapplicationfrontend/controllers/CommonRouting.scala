package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers

import play.api.mvc.{Call, Results}
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.AgentSession._
import uk.gov.hmrc.agentoverseasapplicationfrontend.services.SessionStoreService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait CommonRouting { this: Results =>

  val sessionStoreService: SessionStoreService

  def lookupNextPage(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Call] =
    sessionStoreService.fetchAgentSession.map {
      case MissingAmlsDetails() => routes.ApplicationController.showAntiMoneyLaunderingForm()
      case MissingContactDetails() =>
        routes.ApplicationController.showContactDetailsForm()
      case MissingTradingName() =>
        routes.ApplicationController.showTradingNameForm()
      case MissingMainBusinessAddress() => routes.ApplicationController.showMainBusinessAddressForm()
      case _                            => routes.ApplicationController.showAntiMoneyLaunderingForm()
    }

}
