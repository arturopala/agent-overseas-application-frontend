package uk.gov.hmrc.agentoverseasapplicationfrontend.models

import java.time.LocalDateTime

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class MaintainerDetails(reviewedDate: LocalDateTime)

object MaintainerDetails {
  implicit val format = Json.format[MaintainerDetails]
}

case class ApplicationEntityDetails(
  applicationCreationDate: LocalDateTime,
  status: ApplicationStatus,
  tradingName: String,
  businessEmail: String,
  maintainerReviewedOn: Option[LocalDateTime])

object ApplicationEntityDetails {
  implicit val reads: Reads[ApplicationEntityDetails] = {

    ((__ \ "createdDate").read[LocalDateTime] and
      (__ \ "status").read[ApplicationStatus] and
      (__ \ "tradingDetails" \ "tradingName").read[String] and
      (__ \ "contactDetails" \ "businessEmail").read[String] and
      (__ \ "maintainerDetails")
        .readNullable[MaintainerDetails])((createdDate, status, name, email, maintainerDetails) =>
      ApplicationEntityDetails(createdDate, status, name, email, maintainerDetails.map(_.reviewedDate)))
  }
}
