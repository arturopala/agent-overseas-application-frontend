package uk.gov.hmrc.agentoverseasapplicationfrontend.models

import play.api.libs.json.Json
import uk.gov.hmrc.http.BadRequestException

sealed trait YesNoUnsure

case object Yes extends YesNoUnsure
case object No extends YesNoUnsure
case object Unsure extends YesNoUnsure

object YesNoUnsure {
  def apply(str: String): YesNoUnsure = str.toLowerCase match {
    case "yes"    => Yes
    case "no"     => No
    case "unsure" => Unsure
    case _        => throw new BadRequestException("Strange form input value")
  }

  def unapply(answer: YesNoUnsure): Option[String] = answer match {
    case Yes    => Some("yes")
    case No     => Some("no")
    case Unsure => Some("unsure")
  }

  implicit val formats = Json.format[YesNoUnsure]
}
