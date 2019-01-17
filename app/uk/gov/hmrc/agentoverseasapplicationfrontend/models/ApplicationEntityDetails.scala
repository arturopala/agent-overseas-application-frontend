package uk.gov.hmrc.agentoverseasapplicationfrontend.models

import java.time.LocalDate

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class ApplicationEntityDetails(
  status: ApplicationStatus,
  tradingName: String,
  businessEmail: String,
  maintainerReviewedOn: Option[LocalDate])

object ApplicationEntityDetails {
  implicit val reads: Reads[ApplicationEntityDetails] = {

    ((__ \ "status").read[ApplicationStatus] and
      (__ \ "application" \ "businessDetail" \ "tradingName").read[String] and
      (__ \ "application" \ "contactDetails" \ "businessEmail").read[String] and
      (__ \ "maintainerReviewedOn").readNullable[LocalDate])(ApplicationEntityDetails.apply _)
  }
}
