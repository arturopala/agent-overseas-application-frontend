package uk.gov.hmrc.agentoverseasapplicationfrontend.forms

import play.api.data.Form
import play.api.data.Forms._
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.YesNo

object RegisteredForUkTaxForm {
  val form: Form[YesNo] = Form(
    mapping(
      "registeredForUkTax" -> nonEmptyText
    )(YesNo.apply)(YesNo.unapply)
  )
}
