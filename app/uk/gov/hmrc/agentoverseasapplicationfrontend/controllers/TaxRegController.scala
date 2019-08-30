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

import javax.inject.{Inject, Singleton}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.auth.{AgentAffinityNoHmrcAsAgentAuthAction, BasicAgentAuthAction}
import uk.gov.hmrc.agentoverseasapplicationfrontend.forms.{AddTrnForm, DoYouWantToAddAnotherTrnForm, TaxRegistrationNumberForm, UpdateTrnForm}
import uk.gov.hmrc.agentoverseasapplicationfrontend.forms.YesNoRadioButtonForms.removeTrnForm
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.{AgentSession, TaxRegistrationNumber, Trn}
import uk.gov.hmrc.agentoverseasapplicationfrontend.services.{ApplicationService, SessionStoreService}
import uk.gov.hmrc.agentoverseasapplicationfrontend.views.html._
import uk.gov.hmrc.agentoverseasapplicationfrontend.utils.toFuture

import scala.collection.immutable.SortedSet
import scala.concurrent.ExecutionContext

@Singleton
class TaxRegController @Inject()(
  val env: Environment,
  validApplicantAction: AgentAffinityNoHmrcAsAgentAuthAction,
  sessionStoreService: SessionStoreService,
  applicationService: ApplicationService,
  basicAgentAuthAction: BasicAgentAuthAction)(
  implicit configuration: Configuration,
  messagesApi: MessagesApi,
  override val ec: ExecutionContext)
    extends AgentOverseasBaseController(sessionStoreService, applicationService) with SessionBehaviour
    with I18nSupport {

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

          val (updatedSession, redirectLink) =
            if (validForm.canProvideTaxRegNo.contains(true)) {
              (
                request.agentSession.copy(
                  hasTaxRegNumbers = validForm.canProvideTaxRegNo,
                  taxRegistrationNumbers = validForm.value.flatMap(taxId => Some(SortedSet(taxId))),
                  hasTrnsChanged = validForm.value.isDefined
                ),
                routes.TaxRegController.showYourTaxRegNumbersForm().url)
            } else {
              (
                request.agentSession.copy(
                  hasTaxRegNumbers = None,
                  taxRegistrationNumbers = None,
                  trnUploadStatus = None,
                  hasTrnsChanged = false
                ),
                routes.TaxRegController.showMoreInformationNeeded().url)
            }

          updateSession(updatedSession)(redirectLink)
        }
      )
  }

  def showMoreInformationNeeded: Action[AnyContent] = validApplicantAction.async { implicit request =>
    Ok(tax_more_info_needed())
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
          updateSession(
            request.agentSession
              .copy(
                taxRegistrationNumbers = Some(trns),
                hasTaxRegNumbers = Some(true),
                changingAnswers = false,
                hasTrnsChanged = true))(routes.TaxRegController.showYourTaxRegNumbersForm().url)
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
            case Some(true) => Redirect(routes.TaxRegController.showAddTaxRegNoForm().url)
            case _ =>
              if (request.agentSession.hasTrnsChanged) {
                updateSession(request.agentSession.copy(trnUploadStatus = None, hasTrnsChanged = false))(
                  routes.FileUploadController.showTrnUploadForm().url)
              } else {
                updateSession(request.agentSession.copy(hasTrnsChanged = false))(
                  routes.ApplicationController.showCheckYourAnswers().url)
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

              updateSession(
                request.agentSession
                  .copy(taxRegistrationNumbers = Some(updatedSet), changingAnswers = false, hasTrnsChanged = true))(
                routes.TaxRegController.showYourTaxRegNumbersForm().url)

            case None =>
              Ok(
                update_tax_registration_number(
                  UpdateTrnForm.form.fill(validForm.copy(updated = Some(validForm.original)))))
        }
      )
  }

  def showRemoveTaxRegNumber(trn: String): Action[AnyContent] = validApplicantAction.async { implicit request =>
    if (request.agentSession.taxRegistrationNumbers.exists(_.contains(Trn(trn))))
      Ok(remove_tax_reg_number(removeTrnForm, trn))
    else
      Ok(error_template("global.error.404.title", "global.error.404.heading", "global.error.404.message"))
  }

  def submitRemoveTaxRegNumber(trn: String): Action[AnyContent] = validApplicantAction.async { implicit request =>
    removeTrnForm
      .bindFromRequest()
      .fold(
        formWithErrors => Ok(remove_tax_reg_number(formWithErrors, trn)),
        validForm => {
          if (validForm.value) {
            val updatedSet = request.agentSession.taxRegistrationNumbers
              .fold[SortedSet[Trn]](SortedSet.empty)(trns => trns - Trn(trn))
            val toUpdate: AgentSession =
              if (updatedSet.isEmpty)
                request.agentSession
                  .copy(
                    hasTaxRegNumbers = None,
                    taxRegistrationNumbers = None,
                    trnUploadStatus = None,
                    changingAnswers = false,
                    hasTrnsChanged = true)
              else
                request.agentSession
                  .copy(taxRegistrationNumbers = Some(updatedSet), changingAnswers = false, hasTrnsChanged = true)

            val redirectUrl =
              if (updatedSet.nonEmpty) routes.TaxRegController.showYourTaxRegNumbersForm().url
              else routes.TaxRegController.showTaxRegistrationNumberForm().url
            updateSession(toUpdate)(redirectUrl)
          } else {
            Redirect(routes.TaxRegController.showYourTaxRegNumbersForm())
          }
        }
      )
  }

}
