package uk.gov.hmrc.agentoverseasapplicationfrontend.forms

import play.api.data.Form
import play.api.data.Forms._
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.RadioConfirm
import uk.gov.hmrc.agentoverseasapplicationfrontend.validators.CommonValidators.radioInputSelected

object RegisteredForUkTaxForm {
  val form: Form[RadioConfirm] = Form(
    mapping(
      "registeredForUkTax" -> optional(boolean).verifying(
        radioInputSelected("error.registeredForUkTaxForm.no-radio.selected"))
    )(RadioConfirm.apply)(RadioConfirm.unapply)
  )
}
