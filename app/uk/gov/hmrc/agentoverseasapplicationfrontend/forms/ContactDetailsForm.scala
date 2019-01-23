package uk.gov.hmrc.agentoverseasapplicationfrontend.forms

import play.api.data.Form
import play.api.data.Forms._
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.ContactDetails
import uk.gov.hmrc.agentoverseasapplicationfrontend.validators.CommonValidators._

object ContactDetailsForm {

  def form: Form[ContactDetails] =
    Form[ContactDetails](
      mapping(
        "firstName"         -> firstName,
        "lastName"          -> lastName,
        "jobTitle"          -> jobTitle,
        "businessTelephone" -> businessTelephone,
        "businessEmail"     -> businessEmail
      )(ContactDetails.apply)(ContactDetails.unapply))

}
