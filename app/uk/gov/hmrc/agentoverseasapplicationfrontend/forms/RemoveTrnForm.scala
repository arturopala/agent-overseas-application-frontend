package uk.gov.hmrc.agentoverseasapplicationfrontend.forms

import play.api.data.Form
import play.api.data.Forms.{boolean, mapping, optional}
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.RadioConfirm
import uk.gov.hmrc.agentoverseasapplicationfrontend.validators.CommonValidators.radioInputSelected

object RemoveTrnForm {
  val form: Form[RadioConfirm] = Form(
    mapping("isRemovingTrn" -> optional(boolean).verifying(radioInputSelected("error.removeTrn.no-radio.selected")))(
      RadioConfirm.apply)(RadioConfirm.unapply)
  )
}
