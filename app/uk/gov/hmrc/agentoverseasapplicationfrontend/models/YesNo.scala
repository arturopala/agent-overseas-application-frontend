package uk.gov.hmrc.agentoverseasapplicationfrontend.models

import play.api.libs.json.Json
import uk.gov.hmrc.http.BadRequestException

case class RadioConfirm(value: Option[Boolean])

sealed trait YesNo

case object Yes extends YesNo
case object No extends YesNo

object YesNo {
  def apply(str: String): YesNo = str.toLowerCase match {
    case "yes" => Yes
    case "no"  => No
    case _     => throw new BadRequestException("Strange form input value")
  }

  def unapply(answer: YesNo): Option[String] = answer match {
    case Yes => Some("yes")
    case No  => Some("no")
  }

  implicit val formats = Json.format[YesNo]

  def apply(radioConfirm: RadioConfirm): YesNo = radioConfirm.value match {
    case Some(true) => Yes
    case _          => No
  }

  def toRadioConfirm(answer: YesNo): RadioConfirm = answer match {
    case Yes => RadioConfirm(Some(true))
    case No  => RadioConfirm(Some(false))
  }
}
