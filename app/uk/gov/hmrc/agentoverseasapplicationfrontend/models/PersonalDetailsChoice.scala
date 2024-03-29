/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentoverseasapplicationfrontend.models

import play.api.libs.json._
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.PersonalDetailsChoice.RadioOption
import uk.gov.hmrc.domain.{Nino, SaUtr}
import uk.gov.hmrc.http.BadRequestException

case class PersonalDetailsChoice(choice: Option[RadioOption], nino: Option[Nino], saUtr: Option[SaUtr])

object PersonalDetailsChoice {
  sealed trait RadioOption { val value: String }

  object RadioOption {
    case object NinoChoice extends RadioOption { val value = "nino" }
    case object SaUtrChoice extends RadioOption { val value = "saUtr" }

    def apply(str: String): RadioOption = str.trim match {
      case NinoChoice.value  => NinoChoice
      case SaUtrChoice.value => SaUtrChoice
      case _                 => throw new BadRequestException("Strange form input value")
    }

    def unapply(answer: RadioOption): Option[String] = Some(answer.value)

    implicit val formats = Json.format[RadioOption]
  }

  def apply(choice: String, ninoOpt: Option[String], saUtrOpt: Option[String]): PersonalDetailsChoice = {

    val (nino, saUtr) = RadioOption(choice) match {
      case RadioOption.NinoChoice  => (ninoOpt.map(Nino), None)
      case RadioOption.SaUtrChoice => (None, saUtrOpt.map(SaUtr))
    }

    PersonalDetailsChoice(Some(RadioOption(choice)), nino, saUtr)
  }

  implicit val personalDetailsFormat = Json.format[PersonalDetailsChoice]
}
