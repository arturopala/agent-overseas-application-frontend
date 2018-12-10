package uk.gov.hmrc.agentoverseasapplicationfrontend.models

import play.api.libs.json.Json
import uk.gov.hmrc.http.BadRequestException

sealed trait RegisteredWithHmrc

case object Yes extends RegisteredWithHmrc
case object No extends RegisteredWithHmrc
case object Unsure extends RegisteredWithHmrc

object RegisteredWithHmrc {
  def apply(str: String): RegisteredWithHmrc = str.toLowerCase match {
    case "yes"    => Yes
    case "no"     => No
    case "unsure" => Unsure
    case _        => throw new BadRequestException("Strange form input value")
  }

  def unapply(answer: RegisteredWithHmrc): Option[String] = answer match {
    case Yes    => Some("yes")
    case No     => Some("no")
    case Unsure => Some("unsure")
  }

  implicit val formats = Json.format[RegisteredWithHmrc]
}
