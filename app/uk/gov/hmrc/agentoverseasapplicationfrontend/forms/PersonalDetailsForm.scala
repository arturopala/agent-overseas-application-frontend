package uk.gov.hmrc.agentoverseasapplicationfrontend.forms

import play.api.data.Form
import play.api.data.Forms.{mapping, _}
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.PersonalDetails
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfEqual

object PersonalDetailsForm {
  val form: Form[PersonalDetails] = Form(
    mapping(
      "personalDetailsChoice" -> nonEmptyText,
      "nino"                  -> mandatoryIfEqual("personalDetailsChoice", "nino", nonEmptyText),
      "saUtr"                 -> mandatoryIfEqual("personalDetailsChoice", "saUtr", nonEmptyText)
    )(PersonalDetails.apply)(details =>
      Some((details.choice.value, details.nino.map(_.nino), details.saUtr.map(_.utr))))
  )
}
