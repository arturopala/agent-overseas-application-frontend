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
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.agentoverseasapplicationfrontend.config.CountryNamesLoader
import uk.gov.hmrc.agentoverseasapplicationfrontend.config.view.CheckYourAnswers
import uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.auth.{AgentAffinityNoHmrcAsAgentAuthAction, BasicAgentAuthAction}
import uk.gov.hmrc.agentoverseasapplicationfrontend.forms.YesNoRadioButtonForms.{registeredForUkTaxForm, registeredWithHmrcForm}
import uk.gov.hmrc.agentoverseasapplicationfrontend.forms._
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.AgentSession.{IsRegisteredForUkTax, IsRegisteredWithHmrc}
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.{AgentSession, No, Yes, _}
import uk.gov.hmrc.agentoverseasapplicationfrontend.services.{ApplicationService, SessionStoreService}
import uk.gov.hmrc.agentoverseasapplicationfrontend.utils.toFuture
import uk.gov.hmrc.agentoverseasapplicationfrontend.views.html._

import scala.concurrent.ExecutionContext

@Singleton
class ApplicationController @Inject()(
  val env: Environment,
  validApplicantAction: AgentAffinityNoHmrcAsAgentAuthAction,
  sessionStoreService: SessionStoreService,
  applicationService: ApplicationService,
  countryNamesLoader: CountryNamesLoader,
  basicAgentAuthAction: BasicAgentAuthAction,
  @Named("guidancePageApplicationUrl") guidanceApplicationPageUrl: String)(
  implicit configuration: Configuration,
  messagesApi: MessagesApi,
  override val ec: ExecutionContext)
    extends AgentOverseasBaseController(sessionStoreService, applicationService) with SessionBehaviour
    with I18nSupport {

  private val countries = countryNamesLoader.load

  def showContactDetailsForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    val form = ContactDetailsForm.form
    if (request.agentSession.changingAnswers) {
      Ok(contact_details(request.agentSession.contactDetails.fold(form)(form.fill), Some(showCheckYourAnswersUrl)))
    } else {
      val backLink =
        if (request.agentSession.amlsRequired.getOrElse(false)) routes.FileUploadController.showSuccessfulUploadedForm()
        else routes.AntiMoneyLaunderingController.showMoneyLaunderingRequired()
      Ok(contact_details(request.agentSession.contactDetails.fold(form)(form.fill), Some(backLink.url)))
    }
  }

  def submitContactDetails: Action[AnyContent] = validApplicantAction.async { implicit request =>
    ContactDetailsForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          if (request.agentSession.changingAnswers) {
            Ok(contact_details(formWithErrors, Some(showCheckYourAnswersUrl)))
          } else {
            Ok(contact_details(formWithErrors))
          }
        },
        validForm => {
          updateSession(request.agentSession.copy(contactDetails = Some(validForm)))(
            routes.ApplicationController.showTradingNameForm().url)
        }
      )
  }

  def showTradingNameForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    val form = TradingNameForm.form
    if (request.agentSession.changingAnswers) {
      Ok(trading_name(request.agentSession.tradingName.fold(form)(form.fill), Some(showCheckYourAnswersUrl)))
    } else {
      Ok(trading_name(request.agentSession.tradingName.fold(form)(form.fill)))
    }
  }

  def submitTradingName: Action[AnyContent] = validApplicantAction.async { implicit request =>
    TradingNameForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          if (request.agentSession.changingAnswers) {
            Ok(trading_name(formWithErrors, Some(showCheckYourAnswersUrl)))
          } else {
            Ok(trading_name(formWithErrors))
          }
        },
        validForm =>
          updateSession(request.agentSession.copy(tradingName = Some(validForm)))(
            routes.TradingAddressController.showMainBusinessAddressForm().url)
      )
  }

  def showRegisteredWithHmrcForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    val form = registeredWithHmrcForm
    if (request.agentSession.changingAnswers) {
      Ok(
        registered_with_hmrc(
          request.agentSession.registeredWithHmrc.fold(form)(x => form.fill(YesNo.toRadioConfirm(x))),
          Some(showCheckYourAnswersUrl)))
    } else {
      Ok(registered_with_hmrc(request.agentSession.registeredWithHmrc.fold(form)(x =>
        form.fill(YesNo.toRadioConfirm(x)))))
    }
  }

  def submitRegisteredWithHmrc: Action[AnyContent] = validApplicantAction.async { implicit request =>
    registeredWithHmrcForm
      .bindFromRequest()
      .fold(
        formWithErrors => Ok(registered_with_hmrc(formWithErrors)),
        validFormValue => {
          val newValue = YesNo(validFormValue)
          val redirectTo =
            if (Yes == newValue) routes.ApplicationController.showAgentCodesForm().url
            else routes.ApplicationController.showUkTaxRegistrationForm().url
          val toUpdate = request.agentSession.copy(registeredWithHmrc = Some(newValue))
          if (request.agentSession.changingAnswers) {
            request.agentSession.registeredWithHmrc match {
              case Some(oldValue) =>
                if (oldValue == newValue) {
                  updateSession(request.agentSession.copy(changingAnswers = false))(showCheckYourAnswersUrl)
                } else {
                  updateSession(toUpdate.copy(changingAnswers = false))(redirectTo)
                }
              case None =>
                updateSession(toUpdate.copy(changingAnswers = false))(redirectTo)
            }
          } else {
            updateSession(toUpdate)(redirectTo)
          }
        }
      )
  }

  def showAgentCodesForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    val form = AgentCodesForm.form

    if (request.agentSession.changingAnswers) {
      Ok(
        self_assessment_agent_code(
          request.agentSession.agentCodes.fold(form)(form.fill),
          Some(showCheckYourAnswersUrl)))
    } else {
      Ok(self_assessment_agent_code(request.agentSession.agentCodes.fold(form)(form.fill)))
    }
  }

  def submitAgentCodes: Action[AnyContent] = validApplicantAction.async { implicit request =>
    AgentCodesForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          if (request.agentSession.changingAnswers) {
            Ok(self_assessment_agent_code(formWithErrors, Some(showCheckYourAnswersUrl)))
          } else {
            Ok(self_assessment_agent_code(formWithErrors))
          }
        },
        validFormValue => {
          updateSession(request.agentSession.copy(agentCodes = Some(validFormValue), changingAnswers = false))(
            routes.ApplicationController.showUkTaxRegistrationForm().url)
        }
      )
  }

  def showUkTaxRegistrationForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    val form = registeredForUkTaxForm
    if (request.agentSession.changingAnswers) {
      Ok(
        uk_tax_registration(
          request.agentSession.registeredForUkTax.fold(form)(x => form.fill(YesNo.toRadioConfirm(x))),
          showCheckYourAnswersUrl))
    } else {
      Ok(
        uk_tax_registration(
          request.agentSession.registeredForUkTax.fold(form)(x => form.fill(YesNo.toRadioConfirm(x))),
          ukTaxRegistrationBackLink(request.agentSession).url))
    }
  }

  def submitUkTaxRegistration: Action[AnyContent] = validApplicantAction.async { implicit request =>
    registeredForUkTaxForm
      .bindFromRequest()
      .fold(
        formWithErrors => {
          if (request.agentSession.changingAnswers) {
            Ok(uk_tax_registration(formWithErrors, showCheckYourAnswersUrl))
          } else {
            Ok(uk_tax_registration(formWithErrors, ukTaxRegistrationBackLink(request.agentSession).url))
          }
        },
        validFormValue => {
          val newValue = YesNo(validFormValue)
          val redirectTo =
            if (Yes == newValue) routes.ApplicationController.showPersonalDetailsForm().url
            else routes.ApplicationController.showCompanyRegistrationNumberForm().url
          val toUpdate = request.agentSession.copy(registeredForUkTax = Some(newValue))

          if (request.agentSession.changingAnswers) {
            request.agentSession.registeredForUkTax match {
              case Some(oldValue) =>
                if (oldValue == newValue) {
                  updateSession(request.agentSession.copy(changingAnswers = false))(
                    routes.ApplicationController.showCheckYourAnswers().url)
                } else {
                  updateSession(toUpdate.copy(changingAnswers = false))(redirectTo)
                }
              case None =>
                updateSession(toUpdate.copy(changingAnswers = false))(redirectTo)
            }
          } else {
            updateSession(toUpdate)(redirectTo)
          }
        }
      )
  }

  def showPersonalDetailsForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    val form = PersonalDetailsForm.form
    if (request.agentSession.changingAnswers) {
      Ok(personal_details(request.agentSession.personalDetails.fold(form)(form.fill), Some(showCheckYourAnswersUrl)))
    } else {
      Ok(personal_details(request.agentSession.personalDetails.fold(form)(form.fill)))
    }
  }

  def submitPersonalDetails: Action[AnyContent] = validApplicantAction.async { implicit request =>
    PersonalDetailsForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          if (request.agentSession.changingAnswers) {
            Ok(personal_details(formWithErrors, Some(showCheckYourAnswersUrl)))
          } else {
            Ok(personal_details(formWithErrors))
          }
        },
        validForm => {
          updateSession(request.agentSession.copy(personalDetails = Some(validForm)))(
            routes.ApplicationController.showCompanyRegistrationNumberForm().url)
        }
      )
  }

  def showCompanyRegistrationNumberForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    val form = CompanyRegistrationNumberForm.form
    if (request.agentSession.changingAnswers) {
      Ok(
        company_registration_number(
          request.agentSession.companyRegistrationNumber.fold(form)(form.fill),
          showCheckYourAnswersUrl))
    } else {
      Ok(
        company_registration_number(
          request.agentSession.companyRegistrationNumber.fold(form)(form.fill),
          companyRegNumberBackLink(request.agentSession)))
    }
  }

  def submitCompanyRegistrationNumber: Action[AnyContent] = validApplicantAction.async { implicit request =>
    CompanyRegistrationNumberForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          if (request.agentSession.changingAnswers) {
            Ok(company_registration_number(formWithErrors, showCheckYourAnswersUrl))
          } else {
            Ok(company_registration_number(formWithErrors, companyRegNumberBackLink(request.agentSession)))
          }
        },
        validFormValue => {
          updateSession(request.agentSession.copy(companyRegistrationNumber = Some(validFormValue)))(
            routes.TaxRegController.showTaxRegistrationNumberForm().url)
        }
      )
  }

  private def getCountryName(agentSession: AgentSession): String = {
    val countryCode = agentSession.overseasAddress.map(_.countryCode)
    countryCode
      .flatMap(countries.get)
      .getOrElse(sys.error(s"No country found for code: '${countryCode.getOrElse("")}'"))
  }

  def showCheckYourAnswers: Action[AnyContent] = validApplicantAction.async { implicit request =>
    //make sure user has gone through all the required pages, if not redirect to appropriate page
    sessionStoreService.fetchAgentSession
      .map(lookupNextPage)
      .map { call =>
        if (call == routes.ApplicationController.showCheckYourAnswers() || call == routes.TaxRegController
              .showYourTaxRegNumbersForm()) {

          sessionStoreService.cacheAgentSession(request.agentSession.copy(changingAnswers = false))
          Ok(
            check_your_answers(
              CheckYourAnswers.form,
              CheckYourAnswers(request.agentSession, getCountryName(request.agentSession))))
        } else Redirect(call)
      }
  }

  def submitCheckYourAnswers: Action[AnyContent] = validApplicantAction.async { implicit request =>
    CheckYourAnswers.form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          BadRequest(
            check_your_answers(
              formWithErrors,
              CheckYourAnswers(request.agentSession, getCountryName(request.agentSession)))
          )
        },
        cyaConfirmation => {
          for {
            _ <- applicationService.createApplication(request.agentSession)
            _ <- sessionStoreService.removeAgentSession
          } yield
            Redirect(routes.ApplicationController.showApplicationComplete())
              .flashing(
                "tradingName"   -> request.agentSession.tradingName.getOrElse(""),
                "contactDetail" -> request.agentSession.contactDetails.fold("")(_.businessEmail))
        }
      )

  }

  def showApplicationComplete: Action[AnyContent] = basicAgentAuthAction.async { implicit request =>
    val tradingName = request.flash.get("tradingName")
    val contactDetail = request.flash.get("contactDetail")

    if (tradingName.isDefined && contactDetail.isDefined)
      Ok(application_complete(tradingName.get, contactDetail.get, guidanceApplicationPageUrl))
    else Redirect(routes.AntiMoneyLaunderingController.showAntiMoneyLaunderingForm())
  }

  private def ukTaxRegistrationBackLink(session: AgentSession) = Some(session) match {
    case IsRegisteredWithHmrc(Yes) => routes.ApplicationController.showAgentCodesForm()
    case IsRegisteredWithHmrc(No)  => routes.ApplicationController.showRegisteredWithHmrcForm()
  }

  private def companyRegNumberBackLink(session: AgentSession) = Some(session) match {
    case IsRegisteredForUkTax(Yes) => routes.ApplicationController.showPersonalDetailsForm().url
    case IsRegisteredForUkTax(No)  => routes.ApplicationController.showUkTaxRegistrationForm().url
  }
}
