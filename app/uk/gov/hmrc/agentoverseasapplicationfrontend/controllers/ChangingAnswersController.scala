package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers

import javax.inject.{Inject, Singleton}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.auth.AgentAffinityNoHmrcAsAgentAuthAction
import uk.gov.hmrc.agentoverseasapplicationfrontend.services.SessionStoreService
import uk.gov.hmrc.agentoverseasapplicationfrontend.utils.CountryNamesLoader
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.ExecutionContext

@Singleton
class ChangingAnswersController @Inject()(
  override val messagesApi: MessagesApi,
  val authConnector: AuthConnector,
  val env: Environment,
  validApplicantAction: AgentAffinityNoHmrcAsAgentAuthAction,
  val sessionStoreService: SessionStoreService,
  countryNamesLoader: CountryNamesLoader)(implicit val configuration: Configuration, override val ec: ExecutionContext)
    extends FrontendController with SessionBehaviour with I18nSupport {

  def changeAmlsDetails: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { agentSession =>
      updateSessionAndRedirect(
        agentSession.copy(changingAnswers = true),
        Some(routes.ApplicationController.showAntiMoneyLaunderingForm().url))
    }
  }

  def changeContactDetails: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { agentSession =>
      updateSessionAndRedirect(
        agentSession.copy(changingAnswers = true),
        Some(routes.ApplicationController.showContactDetailsForm().url))
    }
  }

  def changeTradingName: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { agentSession =>
      updateSessionAndRedirect(
        agentSession.copy(changingAnswers = true),
        Some(routes.ApplicationController.showTradingNameForm().url))
    }
  }

  def changeTradingAddress: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { agentSession =>
      updateSessionAndRedirect(
        agentSession.copy(changingAnswers = true),
        Some(routes.ApplicationController.showMainBusinessAddressForm().url))
    }
  }

  def changeRegisteredWithHmrc: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { agentSession =>
      updateSessionAndRedirect(
        agentSession.copy(changingAnswers = true),
        Some(routes.ApplicationController.showRegisteredWithHmrcForm().url))
    }
  }

  def changeAgentCodes: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { agentSession =>
      updateSessionAndRedirect(
        agentSession.copy(changingAnswers = true),
        Some(routes.ApplicationController.showAgentCodesForm().url))
    }
  }

  def changeRegisteredForUKTax: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { agentSession =>
      updateSessionAndRedirect(
        agentSession.copy(changingAnswers = true),
        Some(routes.ApplicationController.showUkTaxRegistrationForm().url))
    }
  }

  def changePersonalDetails: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { agentSession =>
      updateSessionAndRedirect(
        agentSession.copy(changingAnswers = true),
        Some(routes.ApplicationController.showPersonalDetailsForm().url))
    }
  }

  def changeCompanyRegistrationNumber: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { agentSession =>
      updateSessionAndRedirect(
        agentSession.copy(changingAnswers = true),
        Some(routes.ApplicationController.showCompanyRegistrationNumberForm().url))
    }
  }

  def changeYourTaxRegistrationNumbers: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { agentSession =>
      updateSessionAndRedirect(
        agentSession.copy(changingAnswers = true),
        Some(routes.ApplicationController.showYourTaxRegNumbersForm().url))
    }
  }
}
