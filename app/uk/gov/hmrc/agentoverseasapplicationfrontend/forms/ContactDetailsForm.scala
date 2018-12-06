package uk.gov.hmrc.agentoverseasapplicationfrontend.forms

import play.api.data.Form
import play.api.data.Forms._
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.ContactDetails

object ContactDetailsForm {

  def form: Form[ContactDetails] =
    Form[ContactDetails](
      mapping(
        "firstName"         -> nonEmptyText,
        "lastName"          -> nonEmptyText,
        "jobTitle"          -> nonEmptyText,
        "businessTelephone" -> nonEmptyText,
        "businessEmail"     -> nonEmptyText
      )(ContactDetails.apply)(ContactDetails.unapply))

}
