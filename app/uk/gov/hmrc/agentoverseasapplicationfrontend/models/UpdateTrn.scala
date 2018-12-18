package uk.gov.hmrc.agentoverseasapplicationfrontend.models

import play.api.libs.json.{Format, Json}

case class UpdateTrn(original: String, updated: Option[String] = None)

object UpdateTrn {
  implicit val format: Format[UpdateTrn] = Json.format[UpdateTrn]
}
