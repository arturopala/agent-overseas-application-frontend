package uk.gov.hmrc.agentoverseasapplicationfrontend.forms

import play.api.data.Form
import play.api.data.Forms.{boolean, mapping, optional}
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.{TaxRegistrationNumber, Trn}
import uk.gov.hmrc.agentoverseasapplicationfrontend.validators.CommonValidators
import uk.gov.hmrc.agentoverseasapplicationfrontend.validators.CommonValidators.radioInputSelected
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfTrue

object TaxRegistrationNumberForm {

  def form: Form[TaxRegistrationNumber] =
    Form[TaxRegistrationNumber](
      mapping(
        "canProvideTaxRegNo" -> optional(boolean).verifying(radioInputSelected("taxRegNo.form.no-radio.selected")),
        "value"              -> mandatoryIfTrue("canProvideTaxRegNo", CommonValidators.taxRegistrationNumber)
      )((canProvideTaxRegNo, value) => TaxRegistrationNumber(canProvideTaxRegNo, value.map(Trn.apply)))(taxRegNo =>
        Some((taxRegNo.canProvideTaxRegNo, taxRegNo.value.map(_.value)))))
}
