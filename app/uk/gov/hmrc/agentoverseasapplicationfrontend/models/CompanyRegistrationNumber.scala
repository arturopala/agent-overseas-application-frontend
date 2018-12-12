package uk.gov.hmrc.agentoverseasapplicationfrontend.models

import play.api.libs.json._

case class CompanyRegistrationNumber(confirmRegistration: Option[Boolean], registrationNumber: Option[String] = None)

object CompanyRegistrationNumber {
  implicit val format: Format[CompanyRegistrationNumber] = Json.format[CompanyRegistrationNumber]
}