package uk.gov.hmrc.agentoverseasapplicationfrontend.forms

import play.api.data.Form
import play.api.data.Forms._
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.UpdateTrn
import uk.gov.hmrc.agentoverseasapplicationfrontend.validators.CommonValidators.taxRegistrationNumber

object UpdateTrnForm {
  val form: Form[UpdateTrn] = Form[UpdateTrn](
    mapping(
      "original" -> nonEmptyText,
      "updated"  -> optional(taxRegistrationNumber)
    )(UpdateTrn.apply)(UpdateTrn.unapply))
}
