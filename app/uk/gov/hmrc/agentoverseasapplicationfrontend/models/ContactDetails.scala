package uk.gov.hmrc.agentoverseasapplicationfrontend.models

import play.api.libs.json.{Format, Json}

case class ContactDetails(
  firstName: String,
  lastName: String,
  jobTitle: String,
  businessTelephone: String,
  businessEmail: String)

object ContactDetails {
  implicit val contactDetailsFormat: Format[ContactDetails] = Json.format[ContactDetails]
}
