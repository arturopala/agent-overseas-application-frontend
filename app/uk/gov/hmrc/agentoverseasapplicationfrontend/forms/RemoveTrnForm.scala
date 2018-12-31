package uk.gov.hmrc.agentoverseasapplicationfrontend.forms

import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText}
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.YesNoUnsure

object RemoveTrnForm {
  val form: Form[YesNoUnsure] = Form(
    mapping("isRemovingTrn" -> nonEmptyText)(YesNoUnsure.apply)(YesNoUnsure.unapply)
  )
}
