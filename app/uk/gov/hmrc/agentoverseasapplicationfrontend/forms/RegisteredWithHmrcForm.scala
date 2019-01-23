package uk.gov.hmrc.agentoverseasapplicationfrontend.forms

import play.api.data.Form
import play.api.data.Forms._
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.RadioConfirm
import uk.gov.hmrc.agentoverseasapplicationfrontend.validators.CommonValidators._

object RegisteredWithHmrcForm {
  val form: Form[RadioConfirm] = Form(
    mapping(
      "registeredWithHmrc" -> optional(boolean).verifying(
        radioInputSelected("error.registeredWithHmrc.no-radio.selected"))
    )(RadioConfirm.apply)(RadioConfirm.unapply)
  )
}
