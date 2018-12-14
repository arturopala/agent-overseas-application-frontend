package uk.gov.hmrc.agentoverseasapplicationfrontend.models

import play.api.libs.json.{Format, Json}

case class DoYouWantToAddAnotherTrn(value: Option[Boolean])

object DoYouWantToAddAnotherTrn {
  implicit val format: Format[DoYouWantToAddAnotherTrn] = Json.format[DoYouWantToAddAnotherTrn]
}
