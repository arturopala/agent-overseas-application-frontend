package uk.gov.hmrc.agentoverseasapplicationfrontend.models

import play.api.libs.json._
import uk.gov.hmrc.http.BadRequestException

case class RadioConfirm(value: Option[Boolean])

sealed trait YesNo {
  val value: String
}

case object Yes extends YesNo { override val value = "yes" }

case object No extends YesNo { override val value = "no" }

object YesNo {
  def apply(str: String): YesNo = str.toLowerCase match {
    case Yes.value => Yes
    case No.value  => No
    case _         => throw new BadRequestException("Strange form input value")
  }

  def unapply(answer: YesNo): Option[String] = answer match {
    case Yes => Some(Yes.value)
    case No  => Some(No.value)
  }

  implicit val reads: Reads[YesNo] = new Reads[YesNo] {
    override def reads(json: JsValue): JsResult[YesNo] =
      json match {
        case JsString(Yes.value) => JsSuccess(Yes)
        case JsString(No.value)  => JsSuccess(No)
        case invalid             => JsError(s"Invalid YesNo value found: $invalid")
      }
  }

  implicit val writes: Writes[YesNo] = new Writes[YesNo] {
    override def writes(o: YesNo): JsValue = JsString(o.value)
  }

  def apply(radioConfirm: RadioConfirm): YesNo = radioConfirm.value match {
    case Some(true) => Yes
    case _          => No
  }

  def toRadioConfirm(answer: YesNo): RadioConfirm = answer match {
    case Yes => RadioConfirm(Some(true))
    case No  => RadioConfirm(Some(false))
  }
}
