package uk.gov.hmrc.agentoverseasapplicationfrontend.forms

import play.api.data.Form
import play.api.data.Forms.{mapping, _}
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.PersonalDetails
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.PersonalDetails.RadioOption
import uk.gov.hmrc.agentoverseasapplicationfrontend.validators.CommonValidators.{nino, radioInputSelected, saUtr}
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfEqual

object PersonalDetailsForm {
  val form: Form[PersonalDetails] = Form(
    mapping(
      "personalDetailsChoice" -> optional(text).verifying(
        radioInputSelected("error.personalDetails.no-radio.selected")),
      "nino"  -> mandatoryIfEqual("personalDetailsChoice", "nino", nino),
      "saUtr" -> mandatoryIfEqual("personalDetailsChoice", "saUtr", saUtr)
    )((choice, nino, saUtr) =>
      PersonalDetails(choice.map(RadioOption.apply), nino.map(Nino.apply), saUtr.map(SaUtr.apply)))(details =>
      Some((details.choice.map(_.value), details.nino.map(_.value), details.saUtr.map(_.value))))
  )
}
