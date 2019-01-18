package uk.gov.hmrc.agentoverseasapplicationfrontend.forms

import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText}
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.YesNo

object RemoveTrnForm {
  val form: Form[YesNo] = Form(
    mapping("isRemovingTrn" -> nonEmptyText)(YesNo.apply)(YesNo.unapply)
  )
}
