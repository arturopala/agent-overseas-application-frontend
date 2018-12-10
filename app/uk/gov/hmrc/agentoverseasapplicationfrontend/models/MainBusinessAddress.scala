package uk.gov.hmrc.agentoverseasapplicationfrontend.models

import play.api.libs.json.{Format, Json}

case class MainBusinessAddress(
  addressLine1: String,
  addressLine2: String,
  addressLine3: Option[String],
  addressLine4: Option[String],
  countryCode: String)

object MainBusinessAddress {
  implicit val format: Format[MainBusinessAddress] = Json.format[MainBusinessAddress]
}
