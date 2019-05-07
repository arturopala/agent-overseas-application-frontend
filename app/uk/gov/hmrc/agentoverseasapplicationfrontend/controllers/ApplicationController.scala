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
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.agentoverseasapplicationfrontend.config.CountryNamesLoader
import uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.auth.{AgentAffinityNoHmrcAsAgentAuthAction, BasicAgentAuthAction}
import uk.gov.hmrc.agentoverseasapplicationfrontend.forms._
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.AgentSession.{IsRegisteredForUkTax, IsRegisteredWithHmrc}
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.{AgentSession, No, Yes, _}
import uk.gov.hmrc.agentoverseasapplicationfrontend.services.{ApplicationService, SessionStoreService}
import uk.gov.hmrc.agentoverseasapplicationfrontend.utils.toFuture
import uk.gov.hmrc.agentoverseasapplicationfrontend.views.html._
import uk.gov.hmrc.http.HeaderCarrier

import scala.collection.immutable.SortedSet
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
  private val validCountryCodes = countries.keys.toSet

  private val showCheckYourAnswersUrl = routes.ApplicationController.showCheckYourAnswers().url

  def showContactDetailsForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    val form = ContactDetailsForm.form
    if (request.agentSession.changingAnswers) {
      Ok(contact_details(request.agentSession.contactDetails.fold(form)(form.fill), Some(showCheckYourAnswersUrl)))
    } else {
      Ok(contact_details(request.agentSession.contactDetails.fold(form)(form.fill)))
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
    val form = RegisteredWithHmrcForm.form
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
    RegisteredWithHmrcForm.form
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
          if (validFormValue.hasOneOrMoreCodes) {
            updateSession(request.agentSession.copy(agentCodes = Some(validFormValue), changingAnswers = false))(
              routes.ApplicationController.showCheckYourAnswers().url)
          } else {
            updateSession(request.agentSession.copy(agentCodes = Some(validFormValue), changingAnswers = false))(
              routes.ApplicationController.showUkTaxRegistrationForm().url)
          }
        }
      )
  }

  def showUkTaxRegistrationForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    val form = RegisteredForUkTaxForm.form
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
    RegisteredForUkTaxForm.form
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
            routes.ApplicationController.showTaxRegistrationNumberForm().url)
        }
      )
  }

  def showTaxRegistrationNumberForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    val storedTrns = request.agentSession.taxRegistrationNumbers.getOrElse(SortedSet.empty[Trn])

    val whichTrnToPopulate = if (storedTrns.size == 1) {
      storedTrns.headOption
    } else {
      None
    }

    val prePopulate = TaxRegistrationNumber(request.agentSession.hasTaxRegNumbers, whichTrnToPopulate)
    Ok(tax_registration_number(TaxRegistrationNumberForm.form.fill(prePopulate)))
  }

  def submitTaxRegistrationNumber: Action[AnyContent] = validApplicantAction.async { implicit request =>
    TaxRegistrationNumberForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => Ok(tax_registration_number(formWithErrors)),
        validForm => {
          val redirectLink = if (validForm.canProvideTaxRegNo.contains(true)) {
            routes.ApplicationController.showYourTaxRegNumbersForm().url
          } else {
            routes.ApplicationController.showCheckYourAnswers().url
          }
          updateSession(
            request.agentSession.copy(
              hasTaxRegNumbers = validForm.canProvideTaxRegNo,
              taxRegistrationNumbers = validForm.value.flatMap(taxId => Some(SortedSet(taxId)))))(redirectLink)
        }
      )
  }

  def showAddTaxRegNoForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    Ok(add_tax_registration_number(AddTrnForm.form))
  }

  def submitAddTaxRegNo: Action[AnyContent] = validApplicantAction.async { implicit request =>
    AddTrnForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => Ok(add_tax_registration_number(formWithErrors)),
        validForm => {
          val trns = request.agentSession.taxRegistrationNumbers match {
            case Some(numbers) => numbers + Trn(validForm)
            case None          => SortedSet(validForm).map(Trn.apply)
          }
          updateSession(request.agentSession.copy(taxRegistrationNumbers = Some(trns), hasTaxRegNumbers = Some(true)))(
            routes.ApplicationController.showYourTaxRegNumbersForm().url)
        }
      )
  }

  def showYourTaxRegNumbersForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    val trns = request.agentSession.taxRegistrationNumbers.getOrElse(SortedSet.empty[Trn])
    if (request.agentSession.changingAnswers) {
      Ok(
        your_tax_registration_numbers(
          DoYouWantToAddAnotherTrnForm.form,
          trns.map(_.value),
          Some(showCheckYourAnswersUrl)))
    } else {
      Ok(your_tax_registration_numbers(DoYouWantToAddAnotherTrnForm.form, trns.map(_.value)))
    }
  }

  def submitYourTaxRegNumbers: Action[AnyContent] = validApplicantAction.async { implicit request =>
    DoYouWantToAddAnotherTrnForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          val trns = request.agentSession.taxRegistrationNumbers.getOrElse(SortedSet.empty[Trn])
          if (request.agentSession.changingAnswers) {
            Ok(your_tax_registration_numbers(formWithErrors, trns.map(_.value), Some(showCheckYourAnswersUrl)))
          } else {
            Ok(your_tax_registration_numbers(formWithErrors, trns.map(_.value)))
          }
        },
        validForm => {
          validForm.value match {
            case Some(true) => Redirect(routes.ApplicationController.showAddTaxRegNoForm().url)
            case _ => {
              request.agentSession.taxRegistrationNumbers
                .fold(Redirect(routes.ApplicationController.showCheckYourAnswers().url))(_ =>
                  Redirect(routes.FileUploadController.showTrnUploadForm().url))

            }
          }
        }
      )
  }

  def submitUpdateTaxRegNumber: Action[AnyContent] = validApplicantAction.async { implicit request =>
    UpdateTrnForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          Logger.warn(
            s"error during updating tax registration number ${formWithErrors.errors.map(_.message).mkString(",")}")
          Ok(update_tax_registration_number(formWithErrors))
        },
        validForm =>
          validForm.updated match {
            case Some(updatedTrn) =>
              val updatedSet = request.agentSession.taxRegistrationNumbers
                .fold[SortedSet[Trn]](SortedSet.empty)(trns => trns - Trn(validForm.original) + Trn(updatedTrn))

              updateSession(request.agentSession.copy(taxRegistrationNumbers = Some(updatedSet)))(
                routes.ApplicationController.showYourTaxRegNumbersForm().url)

            case None =>
              Ok(
                update_tax_registration_number(
                  UpdateTrnForm.form.fill(validForm.copy(updated = Some(validForm.original)))))
        }
      )
  }

  def showRemoveTaxRegNumber(trn: String): Action[AnyContent] = validApplicantAction.async { implicit request =>
    if (request.agentSession.taxRegistrationNumbers.exists(_.contains(Trn(trn))))
      Ok(remove_tax_reg_number(RemoveTrnForm.form, trn))
    else
      Ok(error_template("global.error.404.title", "global.error.404.heading", "global.error.404.message"))
  }

  def submitRemoveTaxRegNumber(trn: String): Action[AnyContent] = validApplicantAction.async { implicit request =>
    RemoveTrnForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => Ok(remove_tax_reg_number(formWithErrors, trn)),
        validForm => {
          validForm.value match {
            case Some(true) => {
              val updatedSet = request.agentSession.taxRegistrationNumbers
                .fold[SortedSet[Trn]](SortedSet.empty)(trns => trns - Trn(trn))
              val toUpdate: AgentSession =
                if (updatedSet.isEmpty)
                  request.agentSession
                    .copy(hasTaxRegNumbers = None, taxRegistrationNumbers = None, trnUploadStatus = None)
                else request.agentSession.copy(taxRegistrationNumbers = Some(updatedSet))

              val redirectUrl =
                if (updatedSet.nonEmpty) routes.ApplicationController.showYourTaxRegNumbersForm().url
                else routes.ApplicationController.showTaxRegistrationNumberForm().url
              updateSession(toUpdate)(redirectUrl)
            }
            case _ => Redirect(routes.ApplicationController.showYourTaxRegNumbersForm())
          }
        }
      )
  }

  def showCheckYourAnswers: Action[AnyContent] = validApplicantAction.async { implicit request =>
    //make sure user has gone through all the required pages, if not redirect to appropriate page
    sessionStoreService.fetchAgentSession
      .map(lookupNextPage)
      .map { call =>
        if (call == routes.ApplicationController.showCheckYourAnswers() || call == routes.ApplicationController
              .showYourTaxRegNumbersForm()) {
          val countryCode = request.agentSession.mainBusinessAddress.map(_.countryCode)
          val countryName = countryCode
            .flatMap(countries.get)
            .getOrElse(sys.error(s"No country found for code: '${countryCode.getOrElse("")}'"))

          val backLink =
            if (request.agentSession.agentCodes.exists(_.hasOneOrMoreCodes))
              routes.ApplicationController.showAgentCodesForm().url
            else if (request.agentSession.taxRegistrationNumbers.exists(_.nonEmpty))
              routes.FileUploadController.showSuccessfulUploadedForm().url
            else routes.ApplicationController.showTaxRegistrationNumberForm().url

          Ok(check_your_answers(request.agentSession, countryName, backLink))
        } else Redirect(call)
      }
  }

  def submitCheckYourAnswers: Action[AnyContent] = validApplicantAction.async { implicit request =>
    for {
      _ <- applicationService.createApplication(request.agentSession)
      _ <- sessionStoreService.removeAgentSession
    } yield
      Redirect(routes.ApplicationController.showApplicationComplete())
        .flashing(
          "tradingName"   -> request.agentSession.tradingName.getOrElse(""),
          "contactDetail" -> request.agentSession.contactDetails.fold("")(_.businessEmail))
  }

  def showApplicationComplete: Action[AnyContent] = basicAgentAuthAction.async { implicit request =>
    val tradingName = request.flash.get("tradingName")
    val contactDetail = request.flash.get("contactDetail")

    if (tradingName.isDefined && contactDetail.isDefined)
      Ok(application_complete(tradingName.get, contactDetail.get, guidanceApplicationPageUrl))
    else Redirect(routes.AntiMoneyLaunderingController.showAntiMoneyLaunderingForm())
  }

  private def updateSession(agentSession: AgentSession)(redirectTo: String)(implicit hc: HeaderCarrier) =
    if (agentSession.changingAnswers) {
      updateSessionAndRedirect(agentSession.copy(changingAnswers = false))(showCheckYourAnswersUrl)
    } else {
      updateSessionAndRedirect(agentSession)(redirectTo)
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
