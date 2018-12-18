package uk.gov.hmrc.agentoverseasapplicationfrontend.forms

import play.api.data.Form
import play.api.data.Forms._
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.UpdateTrn

object UpdateTrnForm {
  val form: Form[UpdateTrn] = Form[UpdateTrn](
    mapping(
      "original" -> nonEmptyText,
      "updated"  -> optional(text)
    )(UpdateTrn.apply)(UpdateTrn.unapply))
}
