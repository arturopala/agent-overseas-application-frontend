package uk.gov.hmrc.agentoverseasapplicationfrontend.forms

import play.api.data.Form
import play.api.data.Forms._
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.YesNoUnsure

object RegisteredForUkTaxForm {
  val form: Form[YesNoUnsure] = Form(
    mapping(
      "registeredForUkTax" -> nonEmptyText
    )(YesNoUnsure.apply)(YesNoUnsure.unapply)
  )
}
