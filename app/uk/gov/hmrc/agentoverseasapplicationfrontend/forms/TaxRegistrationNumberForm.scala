package uk.gov.hmrc.agentoverseasapplicationfrontend.forms

import play.api.data.Form
import play.api.data.Forms.{boolean, mapping, nonEmptyText, optional}
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.TaxRegistrationNumber
import uk.gov.hmrc.agentoverseasapplicationfrontend.forms.FormValidators.radioInputSelected
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfTrue

object TaxRegistrationNumberForm {

  def form: Form[TaxRegistrationNumber] =
    Form[TaxRegistrationNumber](
      mapping(
        "canProvideTaxRegNo" -> optional(boolean).verifying(radioInputSelected("taxRegNo.form.no-radio.selected")),
        "value"              -> mandatoryIfTrue("canProvideTaxRegNo", nonEmptyText)
      )(TaxRegistrationNumber.apply)(TaxRegistrationNumber.unapply))
}
