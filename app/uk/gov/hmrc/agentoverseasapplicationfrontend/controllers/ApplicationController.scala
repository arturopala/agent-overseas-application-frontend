package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers

import javax.inject.{Inject, Singleton}

import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.agentoverseasapplicationfrontend.config.{AMLSLoader, CountryNamesLoader}
import uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.auth.AgentAffinityNoHmrcAsAgentAuthAction
import uk.gov.hmrc.agentoverseasapplicationfrontend.forms._
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.AgentSession.{IsRegisteredForUkTax, IsRegisteredWithHmrc}
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.{AgentSession, No, Unsure, Yes, _}
import uk.gov.hmrc.agentoverseasapplicationfrontend.services.SessionStoreService
import uk.gov.hmrc.agentoverseasapplicationfrontend.utils.toFuture
import uk.gov.hmrc.agentoverseasapplicationfrontend.views.html._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.collection.immutable.SortedSet
import scala.concurrent.ExecutionContext

@Singleton
class ApplicationController @Inject()(
  override val messagesApi: MessagesApi,
  val authConnector: AuthConnector,
  val env: Environment,
  validApplicantAction: AgentAffinityNoHmrcAsAgentAuthAction,
  val sessionStoreService: SessionStoreService,
  countryNamesLoader: CountryNamesLoader)(implicit val configuration: Configuration, override val ec: ExecutionContext)
    extends FrontendController with SessionBehaviour with I18nSupport {

  private val countries = countryNamesLoader.load
  private val validCountryCodes = countries.keys.toSet
  private val amlsBodies: Map[String, String] = AMLSLoader.load("/amls.csv")

  private val showCheckYourAnswersUrl = routes.ApplicationController.showCheckYourAnswers().url

  def showAntiMoneyLaunderingForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    val form = AmlsDetailsForm.form
    sessionStoreService.fetchAgentSession.map {
      case Some(session) =>
        if (session.changingAnswers) {
          Ok(
            anti_money_laundering(session.amlsDetails.fold(form)(form.fill), amlsBodies, Some(showCheckYourAnswersUrl)))
        } else {
          Ok(anti_money_laundering(session.amlsDetails.fold(form)(form.fill), amlsBodies))
        }

      case _ => Ok(anti_money_laundering(form, amlsBodies))
    }
  }

  def submitAntiMoneyLaundering: Action[AnyContent] = validApplicantAction.async { implicit request =>
    AmlsDetailsForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          sessionStoreService.fetchAgentSession.map {
            case Some(session) =>
              if (session.changingAnswers) {
                Ok(anti_money_laundering(formWithErrors, amlsBodies, Some(showCheckYourAnswersUrl)))
              } else {
                Ok(anti_money_laundering(formWithErrors, amlsBodies))
              }
            case None => Ok(anti_money_laundering(formWithErrors, amlsBodies))
          }
        },
        validForm => {
          sessionStoreService.fetchAgentSession.flatMap {
            case Some(session) =>
              if (session.changingAnswers) {
                updateSessionAndRedirect(
                  session.copy(amlsDetails = Some(validForm), changingAnswers = false),
                  Some(showCheckYourAnswersUrl))
              } else {
                updateSessionAndRedirect(session.copy(amlsDetails = Some(validForm)))
              }

            case None => updateSessionAndRedirect(AgentSession(Some(validForm)))
          }
        }
      )
  }

  def showContactDetailsForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      val form = ContactDetailsForm.form
      if (session.changingAnswers) {
        Ok(contact_details(session.contactDetails.fold(form)(form.fill), Some(showCheckYourAnswersUrl)))
      } else {
        Ok(contact_details(session.contactDetails.fold(form)(form.fill)))
      }
    }
  }

  def submitContactDetails: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      ContactDetailsForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            if (session.changingAnswers) {
              Ok(contact_details(formWithErrors, Some(showCheckYourAnswersUrl)))
            } else {
              Ok(contact_details(formWithErrors))
            }
          },
          validForm => {
            if (session.changingAnswers) {
              updateSessionAndRedirect(
                session.copy(contactDetails = Some(validForm), changingAnswers = false),
                Some(showCheckYourAnswersUrl))
            } else {
              updateSessionAndRedirect(session.copy(contactDetails = Some(validForm)))
            }
          }
        )
    }
  }

  def showTradingNameForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      val form = TradingNameForm.form
      if (session.changingAnswers) {
        Ok(trading_name(session.tradingName.fold(form)(form.fill), Some(showCheckYourAnswersUrl)))
      } else {
        Ok(trading_name(session.tradingName.fold(form)(form.fill)))
      }
    }
  }

  def submitTradingName: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      TradingNameForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            if (session.changingAnswers) {
              Ok(trading_name(formWithErrors, Some(showCheckYourAnswersUrl)))
            } else {
              Ok(trading_name(formWithErrors))
            }
          },
          validForm =>
            if (session.changingAnswers) {
              updateSessionAndRedirect(
                session.copy(tradingName = Some(validForm), changingAnswers = false),
                Some(showCheckYourAnswersUrl))
            } else {
              updateSessionAndRedirect(session.copy(tradingName = Some(validForm)))
          }
        )
    }
  }

  def showMainBusinessAddressForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      val form = MainBusinessAddressForm.mainBusinessAddressForm(validCountryCodes)
      if (session.changingAnswers) {
        Ok(
          main_business_address(
            session.mainBusinessAddress.fold(form)(form.fill),
            countries,
            Some(showCheckYourAnswersUrl)))
      } else {
        Ok(main_business_address(session.mainBusinessAddress.fold(form)(form.fill), countries))
      }
    }
  }

  def submitMainBusinessAddress: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      MainBusinessAddressForm
        .mainBusinessAddressForm(validCountryCodes)
        .bindFromRequest()
        .fold(
          formWithErrors => {
            if (session.changingAnswers) {
              Ok(main_business_address(formWithErrors, countries, Some(showCheckYourAnswersUrl)))
            } else {
              Ok(main_business_address(formWithErrors, countries))
            }
          },
          validForm => {
            if (session.changingAnswers) {
              updateSessionAndRedirect(
                session.copy(mainBusinessAddress = Some(validForm), changingAnswers = false),
                Some(showCheckYourAnswersUrl))
            } else {
              updateSessionAndRedirect(session.copy(mainBusinessAddress = Some(validForm)))
            }
          }
        )
    }
  }

  def showRegisteredWithHmrcForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      val form = RegisteredWithHmrcForm.form

      if (session.changingAnswers) {
        Ok(registered_with_hmrc(session.registeredWithHmrc.fold(form)(form.fill), Some(showCheckYourAnswersUrl)))
      } else {
        Ok(registered_with_hmrc(session.registeredWithHmrc.fold(form)(form.fill)))
      }
    }
  }

  def submitRegisteredWithHmrc: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      RegisteredWithHmrcForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => Ok(registered_with_hmrc(formWithErrors)),
          validFormValue => {
            if (session.changingAnswers) {
              session.registeredWithHmrc match {
                case Some(oldValue) =>
                  if (oldValue == validFormValue) {
                    updateSessionAndRedirect(
                      session.copy(changingAnswers = false),
                      Some(routes.ApplicationController.showCheckYourAnswers().url))
                  } else {
                    updateSessionAndRedirect(
                      session.copy(registeredWithHmrc = Some(validFormValue), changingAnswers = false))
                  }
                case None =>
                  updateSessionAndRedirect(
                    session.copy(registeredWithHmrc = Some(validFormValue), changingAnswers = false))
              }
            } else {
              updateSessionAndRedirect(session.copy(registeredWithHmrc = Some(validFormValue)))
            }
          }
        )
    }
  }

  def showAgentCodesForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      val form = AgentCodesForm.form

      if (session.changingAnswers) {
        Ok(self_assessment_agent_code(session.agentCodes.fold(form)(form.fill), Some(showCheckYourAnswersUrl)))
      } else {
        Ok(self_assessment_agent_code(session.agentCodes.fold(form)(form.fill)))
      }
    }
  }

  def submitAgentCodes: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      AgentCodesForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            if (session.changingAnswers) {
              Ok(self_assessment_agent_code(formWithErrors, Some(showCheckYourAnswersUrl)))
            } else {
              Ok(self_assessment_agent_code(formWithErrors))
            }
          },
          validFormValue => {
            if (session.changingAnswers && validFormValue.hasOneOrMoreCodes) {
              updateSessionAndRedirect(
                session.copy(agentCodes = Some(validFormValue), changingAnswers = false),
                Some(showCheckYourAnswersUrl))
            } else {
              updateSessionAndRedirect(session.copy(agentCodes = Some(validFormValue), changingAnswers = false))
            }
          }
        )
    }
  }

  def showUkTaxRegistrationForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      val form = RegisteredForUkTaxForm.form
      if (session.changingAnswers) {
        Ok(uk_tax_registration(session.registeredForUkTax.fold(form)(form.fill), showCheckYourAnswersUrl))
      } else {
        Ok(
          uk_tax_registration(session.registeredForUkTax.fold(form)(form.fill), ukTaxRegistrationBackLink(session).url))
      }
    }
  }

  def submitUkTaxRegistration: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      RegisteredForUkTaxForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            if (session.changingAnswers) {
              Ok(uk_tax_registration(formWithErrors, showCheckYourAnswersUrl))
            } else {
              Ok(uk_tax_registration(formWithErrors, ukTaxRegistrationBackLink(session).url))
            }
          },
          validFormValue => {
            if (session.changingAnswers) {
              session.registeredForUkTax match {
                case Some(oldValue) =>
                  if (oldValue == validFormValue) {
                    updateSessionAndRedirect(
                      session.copy(changingAnswers = false),
                      Some(routes.ApplicationController.showCheckYourAnswers().url))
                  } else {
                    updateSessionAndRedirect(
                      session.copy(registeredForUkTax = Some(validFormValue), changingAnswers = false))
                  }
                case None =>
                  updateSessionAndRedirect(
                    session.copy(registeredForUkTax = Some(validFormValue), changingAnswers = false))
              }
            } else {
              updateSessionAndRedirect(session.copy(registeredForUkTax = Some(validFormValue)))
            }
          }
        )
    }
  }

  def showPersonalDetailsForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      val form = PersonalDetailsForm.form
      if (session.changingAnswers) {
        Ok(personal_details(session.personalDetails.fold(form)(form.fill), Some(showCheckYourAnswersUrl)))
      } else {
        Ok(personal_details(session.personalDetails.fold(form)(form.fill)))
      }
    }
  }

  def submitPersonalDetails: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      PersonalDetailsForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            if (session.changingAnswers) {
              Ok(personal_details(formWithErrors, Some(showCheckYourAnswersUrl)))
            } else {
              Ok(personal_details(formWithErrors))
            }
          },
          validForm => {
            if (session.changingAnswers) {
              updateSessionAndRedirect(
                session.copy(personalDetails = Some(validForm), changingAnswers = false),
                Some(showCheckYourAnswersUrl))
            } else {
              updateSessionAndRedirect(session.copy(personalDetails = Some(validForm)))
            }
          }
        )
    }
  }

  def showCompanyRegistrationNumberForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      val form = CompanyRegistrationNumberForm.form
      if (session.changingAnswers) {
        Ok(
          company_registration_number(session.companyRegistrationNumber.fold(form)(form.fill), showCheckYourAnswersUrl))
      } else {
        Ok(
          company_registration_number(
            session.companyRegistrationNumber.fold(form)(form.fill),
            companyRegNumberBackLink(session)))
      }
    }
  }

  def submitCompanyRegistrationNumber: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      CompanyRegistrationNumberForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            if (session.changingAnswers) {
              Ok(company_registration_number(formWithErrors, showCheckYourAnswersUrl))
            } else {
              Ok(company_registration_number(formWithErrors, companyRegNumberBackLink(session)))
            }
          },
          validFormValue => {
            if (session.changingAnswers) {
              updateSessionAndRedirect(
                session.copy(companyRegistrationNumber = Some(validFormValue), changingAnswers = false),
                Some(showCheckYourAnswersUrl))
            } else {
              updateSessionAndRedirect(session.copy(companyRegistrationNumber = Some(validFormValue)))
            }
          }
        )
    }
  }

  def showTaxRegistrationNumberForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { applicationSession =>
      val storedTrns = applicationSession.taxRegistrationNumbers.getOrElse(SortedSet.empty[String])

      val whichTrnToPopulate = if (storedTrns.size == 1) {
        storedTrns.headOption
      } else {
        None
      }

      val prePopulate = TaxRegistrationNumber(applicationSession.hasTaxRegNumbers, whichTrnToPopulate)
      Ok(tax_registration_number(TaxRegistrationNumberForm.form.fill(prePopulate)))
    }
  }

  def submitTaxRegistrationNumber: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { applicationData =>
      TaxRegistrationNumberForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => Ok(tax_registration_number(formWithErrors)),
          validForm =>
            updateSessionAndRedirect(
              applicationData.copy(
                hasTaxRegNumbers = validForm.canProvideTaxRegNo,
                taxRegistrationNumbers = validForm.value.flatMap(taxId => Some(SortedSet(taxId)))))
        )
    }
  }

  def showAddTaxRegNoForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { _ =>
      Ok(add_tax_registration_number(AddTrnForm.form))
    }
  }

  def submitAddTaxRegNo: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      AddTrnForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => Ok(add_tax_registration_number(formWithErrors)),
          validForm => {
            val trns = session.taxRegistrationNumbers match {
              case Some(numbers) => numbers + validForm
              case None          => SortedSet(validForm)
            }
            updateSessionAndRedirect(
              session.copy(taxRegistrationNumbers = Some(trns)),
              Some(routes.ApplicationController.showYourTaxRegNumbersForm().url))
          }
        )
    }
  }

  def showYourTaxRegNumbersForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      val trns = session.taxRegistrationNumbers.getOrElse(SortedSet.empty)
      if (session.changingAnswers) {
        Ok(your_tax_registration_numbers(DoYouWantToAddAnotherTrnForm.form, trns, Some(showCheckYourAnswersUrl)))
      } else {
        Ok(your_tax_registration_numbers(DoYouWantToAddAnotherTrnForm.form, trns))
      }
    }
  }

  def submitYourTaxRegNumbers: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      DoYouWantToAddAnotherTrnForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            val trns = session.taxRegistrationNumbers.getOrElse(SortedSet.empty)
            if (session.changingAnswers) {
              Ok(your_tax_registration_numbers(formWithErrors, trns, Some(showCheckYourAnswersUrl)))
            } else {
              Ok(your_tax_registration_numbers(formWithErrors, trns))
            }
          },
          validForm => {
            validForm.value match {
              case Some(true) => Redirect(routes.ApplicationController.showAddTaxRegNoForm().url)
              case _          => Redirect(routes.ApplicationController.showCheckYourAnswers().url)
            }
          }
        )
    }
  }

  def submitUpdateTaxRegNumber: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { session =>
      UpdateTrnForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            Logger.warn(
              s"error during updating tax registration number ${formWithErrors.errors.map(_.message).mkString(",")}")
            InternalServerError
          },
          validForm =>
            validForm.updated match {
              case Some(updatedTrn) =>
                val updatedSet = session.taxRegistrationNumbers.fold[SortedSet[String]](SortedSet.empty)(trns =>
                  trns - validForm.original + updatedTrn)

                updateSessionAndRedirect(
                  session.copy(taxRegistrationNumbers = Some(updatedSet)),
                  Some(routes.ApplicationController.showYourTaxRegNumbersForm().url))

              case None =>
                Ok(
                  update_tax_registration_number(
                    UpdateTrnForm.form.fill(validForm.copy(updated = Some(validForm.original)))))
          }
        )
    }
  }

  def showRemoveTaxRegNumber(trn: String): Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { applicantSession =>
      if (applicantSession.taxRegistrationNumbers.exists(_.contains(trn)))
        Ok(remove_tax_reg_number(RemoveTrnForm.form, trn))
      else
        Ok(error_template("global.error.404.title", "global.error.404.heading", "global.error.404.message"))
    }
  }

  def submitRemoveTaxRegNumber(trn: String): Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { applicantSession =>
      RemoveTrnForm.form
        .bindFromRequest()
        .fold(
          formWithErrors => Ok(remove_tax_reg_number(formWithErrors, trn)),
          validForm => {
            validForm match {
              case Yes => {
                val updatedSet = applicantSession.taxRegistrationNumbers
                  .fold[SortedSet[String]](SortedSet.empty)(trns => trns - trn)

                val updateSession: AgentSession =
                  if (updatedSet.isEmpty) applicantSession.copy(hasTaxRegNumbers = None, taxRegistrationNumbers = None)
                  else applicantSession.copy(taxRegistrationNumbers = Some(updatedSet))

                updateSessionAndRedirect(
                  updateSession,
                  if (updatedSet.nonEmpty) Some(routes.ApplicationController.showYourTaxRegNumbersForm().url)
                  else Some(routes.ApplicationController.showTaxRegistrationNumberForm().url)
                )
              }
              case _ => Redirect(routes.ApplicationController.showYourTaxRegNumbersForm())
            }
          }
        )
    }
  }

  def showCheckYourAnswers: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { applicationSession =>
      //make sure user has gone through all the required pages, if not redirect to appropriate page
      lookupNextPage.map { call =>
        if (call == routes.ApplicationController.showCheckYourAnswers() || call == routes.ApplicationController
              .showYourTaxRegNumbersForm()) {
          val countryCode = applicationSession.mainBusinessAddress.map(_.countryCode)
          val countryName = countryCode
            .flatMap(countries.get)
            .getOrElse(sys.error(s"No country found for code: '${countryCode.getOrElse("")}'"))
          Ok(check_your_answers(applicationSession, countryName))
        } else {
          Redirect(call)
        }
      }
    }
  }

  def submitCheckYourAnswers: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { applicationSession =>
      Redirect(routes.ApplicationController.showApplicationComplete())
    }
  }

  def showApplicationComplete: Action[AnyContent] = validApplicantAction.async { implicit request =>
    withAgentSession { applicationSession =>
      Ok(application_complete(applicationSession))
    }
  }

  private def ukTaxRegistrationBackLink(session: AgentSession) = Some(session) match {
    case IsRegisteredWithHmrc(Yes)         => routes.ApplicationController.showAgentCodesForm()
    case IsRegisteredWithHmrc(No | Unsure) => routes.ApplicationController.showRegisteredWithHmrcForm()
  }

  private def companyRegNumberBackLink(session: AgentSession) = Some(session) match {
    case IsRegisteredForUkTax(Yes)         => routes.ApplicationController.showPersonalDetailsForm().url
    case IsRegisteredForUkTax(No | Unsure) => routes.ApplicationController.showUkTaxRegistrationForm().url
  }
}
