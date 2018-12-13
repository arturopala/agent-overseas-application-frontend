package uk.gov.hmrc.agentoverseasapplicationfrontend.models

import play.api.libs.json.Json

case class AgentCodes(
  selfAssessment: Option[String],
  corporationTax: Option[String],
  vat: Option[String],
  paye: Option[String]) {
  def hasOneOrMoreCodes: Boolean = this match {
    case AgentCodes(None, None, None, None) => false
    case _                                  => true
  }
}

object AgentCodes {
  implicit val format = Json.format[AgentCodes]
}
