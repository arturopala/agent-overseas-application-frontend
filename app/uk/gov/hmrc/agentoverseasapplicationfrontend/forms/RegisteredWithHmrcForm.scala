package uk.gov.hmrc.agentoverseasapplicationfrontend.forms

import play.api.data.Form
import play.api.data.Forms._
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.RegisteredWithHmrc

object RegisteredWithHmrcForm {
  val form: Form[RegisteredWithHmrc] = Form(
    mapping(
      "registeredWithHmrc" -> nonEmptyText
    )(RegisteredWithHmrc.apply)(RegisteredWithHmrc.unapply)
  )
}
