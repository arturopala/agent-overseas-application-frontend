package uk.gov.hmrc.agentoverseasapplicationfrontend.forms

import play.api.data.Form
import play.api.data.Forms._
import uk.gov.hmrc.agentoverseasapplicationfrontend.validators.CommonValidators._
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.{CompanyRegistrationNumber, Crn}
import uk.gov.hmrc.agentoverseasapplicationfrontend.validators.CommonValidators
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfTrue

object CompanyRegistrationNumberForm {

  def form: Form[CompanyRegistrationNumber] =
    Form[CompanyRegistrationNumber](
      mapping(
        "confirmRegistration" -> optional(boolean).verifying(
          radioInputSelected("companyRegistrationNumber.error.no-radio.selected")),
        "registrationNumber" -> mandatoryIfTrue("confirmRegistration", CommonValidators.companyRegistrationNumber)
      )((confirmRegistration, registrationNumber) =>
        CompanyRegistrationNumber(confirmRegistration, registrationNumber.map(Crn.apply)))(crn =>
        Some((crn.confirmRegistration, crn.registrationNumber.map(_.value)))))
}
