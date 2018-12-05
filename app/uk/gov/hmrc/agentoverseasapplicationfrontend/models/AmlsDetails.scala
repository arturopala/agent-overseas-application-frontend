package uk.gov.hmrc.agentoverseasapplicationfrontend.models

import play.api.libs.json.Json

case class AmlsDetails(supervisoryBody: String, membershipNumber: Option[String])

object AmlsDetails {
  implicit val format = Json.format[AmlsDetails]
}
