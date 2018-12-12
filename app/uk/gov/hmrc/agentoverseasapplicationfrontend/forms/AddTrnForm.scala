package uk.gov.hmrc.agentoverseasapplicationfrontend.forms

import play.api.data.Form
import play.api.data.Forms._

object AddTrnForm {
  val form: Form[String] = Form(
    single(
      "trn" -> nonEmptyText
    )
  )
}
