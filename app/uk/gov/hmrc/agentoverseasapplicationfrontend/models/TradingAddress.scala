package uk.gov.hmrc.agentoverseasapplicationfrontend.models

import play.api.libs.json.{Format, Json}

case class TradingAddress(
  addressLine1: String,
  addressLine2: String,
  addressLine3: Option[String],
  addressLine4: Option[String],
  countryCode: String)

object TradingAddress {
  implicit val format: Format[TradingAddress] = Json.format[TradingAddress]
}
