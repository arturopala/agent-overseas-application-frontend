package uk.gov.hmrc.agentoverseasapplicationfrontend.forms

import play.api.data.Form
import play.api.data.Forms._
import uk.gov.hmrc.agentoverseasapplicationfrontend.forms.FormValidators.radioInputSelected
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.DoYouWantToAddAnotherTrn

object DoYouWantToAddAnotherTrnForm {

  val form: Form[DoYouWantToAddAnotherTrn] = Form(
    mapping(
      "value" -> optional(boolean).verifying(radioInputSelected("doYouWantToAddAnotherTrn.error.no-radio.selected"))
    )(DoYouWantToAddAnotherTrn.apply)(DoYouWantToAddAnotherTrn.unapply)
  )
}
