package uk.gov.hmrc.agentoverseasapplicationfrontend.models

import play.api.libs.json.Json

case class AgentCodes(selfAssessment: Option[SaAgentCode], corporationTax: Option[CtAgentCode]) {
  def hasOneOrMoreCodes: Boolean = this match {
    case AgentCodes(None, None) => false
    case _                      => true
  }

  def isEmpty: Boolean = !hasOneOrMoreCodes
}

object AgentCodes {
  implicit val format = Json.format[AgentCodes]
}
