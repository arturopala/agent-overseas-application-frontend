package uk.gov.hmrc.agentoverseasapplicationfrontend.models

import play.api.libs.json._
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.PersonalDetails.RadioOption
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http.BadRequestException

case class PersonalDetails(choice: RadioOption, nino: Option[Nino], saUtr: Option[SaUtr])

object PersonalDetails {
  sealed trait RadioOption { val value: String }

  object RadioOption {
    case object NinoChoice extends RadioOption { val value = "nino" }
    case object SaUtrChoice extends RadioOption { val value = "saUtr" }
    case object Unsure extends RadioOption { val value = "unsure" }

    def apply(str: String): RadioOption = str.trim match {
      case NinoChoice.value  => NinoChoice
      case SaUtrChoice.value => SaUtrChoice
      case Unsure.value      => Unsure
      case _                 => throw new BadRequestException("Strange form input value")
    }

    def unapply(answer: RadioOption): Option[String] = Some(answer.value)

    implicit val formats = Json.format[RadioOption]
  }

  def apply(choice: String, ninoOpt: Option[String], saUtrOpt: Option[String]): PersonalDetails = {

    val (nino, saUtr) = RadioOption(choice) match {
      case RadioOption.NinoChoice  => (ninoOpt.map(Nino), None)
      case RadioOption.SaUtrChoice => (None, saUtrOpt.map(SaUtr))
      case RadioOption.Unsure      => (None, None)
    }

    PersonalDetails(RadioOption(choice), nino, saUtr)
  }

  implicit val personalDetailsFormat = Json.format[PersonalDetails]
}
