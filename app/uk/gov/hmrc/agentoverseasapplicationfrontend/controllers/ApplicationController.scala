package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers

import javax.inject.{Inject, Singleton}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.agentoverseasapplicationfrontend.config.{AMLSLoader, CountryNamesLoader}
import uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.auth.AgentAffinityNoHmrcAsAgentAuthAction
import uk.gov.hmrc.agentoverseasapplicationfrontend.forms._
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.AgentSession.{IsRegisteredForUkTax, IsRegisteredWithHmrc}
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.ApplicationStatus.Rejected
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.{AgentSession, No, Yes, _}
import uk.gov.hmrc.agentoverseasapplicationfrontend.services.{ApplicationService, SessionStoreService}
import uk.gov.hmrc.agentoverseasapplicationfrontend.utils.toFuture
import uk.gov.hmrc.agentoverseasapplicationfrontend.views.html._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.collection.immutable.SortedSet
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationController @Inject()(
  override val messagesApi: MessagesApi,
  val authConnector: AuthConnector,
  val env: Environment,
  validApplicantAction: AgentAffinityNoHmrcAsAgentAuthAction,
  val sessionStoreService: SessionStoreService,
  val applicationService: ApplicationService,
  countryNamesLoader: CountryNamesLoader)(implicit val configuration: Configuration, override val ec: ExecutionContext)
    extends FrontendController with SessionBehaviour with I18nSupport {

  private val countries = countryNamesLoader.load
  private val validCountryCodes = countries.keys.toSet
  private val amlsBodies: Map[String, String] = AMLSLoader.load("/amls.csv")

  private val showCheckYourAnswersUrl = routes.ApplicationController.showCheckYourAnswers().url

  def showAntiMoneyLaunderingForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    val form = AmlsDetailsForm.form(amlsBodies.values.toSet)

    val backUrl: Future[Option[String]] = {
      if (request.agentSession.changingAnswers) Future.successful(Some(showCheckYourAnswersUrl))
      else
        applicationService.getCurrentApplication.map {
          case Some(application) if application.status == Rejected =>
            Some(routes.StartController.applicationStatus().url)
          case _ => None
        }
    }

    backUrl.map(url =>
      Ok(anti_money_laundering(request.agentSession.amlsDetails.fold(form)(form.fill), amlsBodies, url)))
  }

  def submitAntiMoneyLaundering: Action[AnyContent] = validApplicantAction.async { implicit request =>
    AmlsDetailsForm
      .form(amlsBodies.values.toSet)
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
          if (request.agentSession.changingAnswers) {
            updateSessionAndRedirect(
              request.agentSession.copy(contactDetails = Some(validForm), changingAnswers = false),
              Some(showCheckYourAnswersUrl))
          } else {
            updateSessionAndRedirect(request.agentSession.copy(contactDetails = Some(validForm)))
          }
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
          if (request.agentSession.changingAnswers) {
            updateSessionAndRedirect(
              request.agentSession.copy(tradingName = Some(validForm), changingAnswers = false),
              Some(showCheckYourAnswersUrl))
          } else {
            updateSessionAndRedirect(request.agentSession.copy(tradingName = Some(validForm)))
        }
      )
  }

  def showMainBusinessAddressForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    val form = MainBusinessAddressForm.mainBusinessAddressForm(validCountryCodes)
    if (request.agentSession.changingAnswers) {
      Ok(
        main_business_address(
          request.agentSession.mainBusinessAddress.fold(form)(form.fill),
          countries,
          Some(showCheckYourAnswersUrl)))
    } else {
      Ok(main_business_address(request.agentSession.mainBusinessAddress.fold(form)(form.fill), countries))
    }
  }

  def submitMainBusinessAddress: Action[AnyContent] = validApplicantAction.async { implicit request =>
    MainBusinessAddressForm
      .mainBusinessAddressForm(validCountryCodes)
      .bindFromRequest()
      .fold(
        formWithErrors => {
          if (request.agentSession.changingAnswers) {
            Ok(main_business_address(formWithErrors, countries, Some(showCheckYourAnswersUrl)))
          } else {
            Ok(main_business_address(formWithErrors, countries))
          }
        },
        validForm => {
          if (request.agentSession.changingAnswers) {
            updateSessionAndRedirect(
              request.agentSession.copy(mainBusinessAddress = Some(validForm), changingAnswers = false),
              Some(showCheckYourAnswersUrl))
          } else {
            updateSessionAndRedirect(request.agentSession.copy(mainBusinessAddress = Some(validForm)))
          }
        }
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
          if (request.agentSession.changingAnswers) {
            request.agentSession.registeredWithHmrc match {
              case Some(oldValue) =>
                if (oldValue == YesNo(validFormValue)) {
                  updateSessionAndRedirect(
                    request.agentSession.copy(changingAnswers = false),
                    Some(routes.ApplicationController.showCheckYourAnswers().url))
                } else {
                  updateSessionAndRedirect(
                    request.agentSession
                      .copy(registeredWithHmrc = Some(YesNo(validFormValue)), changingAnswers = false))
                }
              case None =>
                updateSessionAndRedirect(
                  request.agentSession.copy(registeredWithHmrc = Some(YesNo(validFormValue)), changingAnswers = false))
            }
          } else {
            updateSessionAndRedirect(request.agentSession.copy(registeredWithHmrc = Some(YesNo(validFormValue))))
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
          if (request.agentSession.changingAnswers && validFormValue.hasOneOrMoreCodes) {
            updateSessionAndRedirect(
              request.agentSession.copy(agentCodes = Some(validFormValue), changingAnswers = false),
              Some(showCheckYourAnswersUrl))
          } else {
            updateSessionAndRedirect(
              request.agentSession.copy(agentCodes = Some(validFormValue), changingAnswers = false))
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
          if (request.agentSession.changingAnswers) {
            request.agentSession.registeredForUkTax match {
              case Some(oldValue) =>
                if (oldValue == YesNo(validFormValue)) {
                  updateSessionAndRedirect(
                    request.agentSession.copy(changingAnswers = false),
                    Some(routes.ApplicationController.showCheckYourAnswers().url))
                } else {
                  updateSessionAndRedirect(
                    request.agentSession
                      .copy(registeredForUkTax = Some(YesNo(validFormValue)), changingAnswers = false))
                }
              case None =>
                updateSessionAndRedirect(
                  request.agentSession.copy(registeredForUkTax = Some(YesNo(validFormValue)), changingAnswers = false))
            }
          } else {
            updateSessionAndRedirect(request.agentSession.copy(registeredForUkTax = Some(YesNo(validFormValue))))
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
          if (request.agentSession.changingAnswers) {
            updateSessionAndRedirect(
              request.agentSession.copy(personalDetails = Some(validForm), changingAnswers = false),
              Some(showCheckYourAnswersUrl))
          } else {
            updateSessionAndRedirect(request.agentSession.copy(personalDetails = Some(validForm)))
          }
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
          if (request.agentSession.changingAnswers) {
            updateSessionAndRedirect(
              request.agentSession.copy(companyRegistrationNumber = Some(validFormValue), changingAnswers = false),
              Some(showCheckYourAnswersUrl))
          } else {
            updateSessionAndRedirect(request.agentSession.copy(companyRegistrationNumber = Some(validFormValue)))
          }
        }
      )
  }

  def showTaxRegistrationNumberForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    val storedTrns = request.agentSession.taxRegistrationNumbers.getOrElse(SortedSet.empty[String])

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
        validForm =>
          updateSessionAndRedirect(
            request.agentSession.copy(
              hasTaxRegNumbers = validForm.canProvideTaxRegNo,
              taxRegistrationNumbers = validForm.value.flatMap(taxId => Some(SortedSet(taxId)))))
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
            case Some(numbers) => numbers + validForm
            case None          => SortedSet(validForm)
          }
          updateSessionAndRedirect(
            request.agentSession.copy(taxRegistrationNumbers = Some(trns)),
            Some(routes.ApplicationController.showYourTaxRegNumbersForm().url))
        }
      )
  }

  def showYourTaxRegNumbersForm: Action[AnyContent] = validApplicantAction.async { implicit request =>
    val trns = request.agentSession.taxRegistrationNumbers.getOrElse(SortedSet.empty[String])
    if (request.agentSession.changingAnswers) {
      Ok(your_tax_registration_numbers(DoYouWantToAddAnotherTrnForm.form, trns, Some(showCheckYourAnswersUrl)))
    } else {
      Ok(your_tax_registration_numbers(DoYouWantToAddAnotherTrnForm.form, trns))
    }
  }

  def submitYourTaxRegNumbers: Action[AnyContent] = validApplicantAction.async { implicit request =>
    DoYouWantToAddAnotherTrnForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          val trns = request.agentSession.taxRegistrationNumbers.getOrElse(SortedSet.empty[String])
          if (request.agentSession.changingAnswers) {
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
                .fold[SortedSet[String]](SortedSet.empty)(trns => trns - validForm.original + updatedTrn)

              updateSessionAndRedirect(
                request.agentSession.copy(taxRegistrationNumbers = Some(updatedSet)),
                Some(routes.ApplicationController.showYourTaxRegNumbersForm().url))

            case None =>
              Ok(
                update_tax_registration_number(
                  UpdateTrnForm.form.fill(validForm.copy(updated = Some(validForm.original)))))
        }
      )
  }

  def showRemoveTaxRegNumber(trn: String): Action[AnyContent] = validApplicantAction.async { implicit request =>
    if (request.agentSession.taxRegistrationNumbers.exists(_.contains(trn)))
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
                .fold[SortedSet[String]](SortedSet.empty)(trns => trns - trn)

              val updateSession: AgentSession =
                if (updatedSet.isEmpty)
                  request.agentSession.copy(hasTaxRegNumbers = None, taxRegistrationNumbers = None)
                else request.agentSession.copy(taxRegistrationNumbers = Some(updatedSet))

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

  def showCheckYourAnswers: Action[AnyContent] = validApplicantAction.async { implicit request =>
    //make sure user has gone through all the required pages, if not redirect to appropriate page
    lookupNextPage.map { call =>
      if (call == routes.ApplicationController.showCheckYourAnswers() || call == routes.ApplicationController
            .showYourTaxRegNumbersForm()) {
        val countryCode = request.agentSession.mainBusinessAddress.map(_.countryCode)
        val countryName = countryCode
          .flatMap(countries.get)
          .getOrElse(sys.error(s"No country found for code: '${countryCode.getOrElse("")}'"))
        Ok(check_your_answers(request.agentSession, countryName))
      } else {
        Redirect(call)
      }
    }
  }

  def submitCheckYourAnswers: Action[AnyContent] = validApplicantAction.async { implicit request =>
    applicationService.createApplication(request.agentSession).map { _ =>
      Redirect(routes.ApplicationController.showApplicationComplete())
    }
  }

  def showApplicationComplete: Action[AnyContent] = validApplicantAction.async { implicit request =>
    Ok(application_complete(request.agentSession))
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
