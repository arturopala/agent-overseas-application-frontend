package uk.gov.hmrc.agentoverseasapplicationfrontend.forms

import play.api.data.Form
import play.api.data.Forms._
import uk.gov.hmrc.agentoverseasapplicationfrontend.validators.CommonValidators

object AddTrnForm {
  val form: Form[String] = Form(
    single(
      "trn" -> CommonValidators.taxRegistrationNumber
    )
  )
}
