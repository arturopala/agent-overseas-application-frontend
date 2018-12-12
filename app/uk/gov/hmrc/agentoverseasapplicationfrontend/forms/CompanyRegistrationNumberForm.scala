package uk.gov.hmrc.agentoverseasapplicationfrontend.forms

import play.api.data.Form
import play.api.data.Forms._
import uk.gov.hmrc.agentoverseasapplicationfrontend.forms.FormValidators.radioInputSelected
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.CompanyRegistrationNumber
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfTrue

object CompanyRegistrationNumberForm {

  def form: Form[CompanyRegistrationNumber] =
    Form[CompanyRegistrationNumber](
      mapping(
        "confirmRegistration" -> optional(boolean).verifying(
          radioInputSelected("companyRegistrationNumber.error.no-radio.selected")),
        "registrationNumber" -> mandatoryIfTrue("confirmRegistration", nonEmptyText)
      )(CompanyRegistrationNumber.apply)(CompanyRegistrationNumber.unapply))
}
