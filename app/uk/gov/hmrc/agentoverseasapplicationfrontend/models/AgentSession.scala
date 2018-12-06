package uk.gov.hmrc.agentoverseasapplicationfrontend.models

import play.api.libs.json.{Json, OFormat}

case class AgentSession(amlsDetails: Option[AmlsDetails] = None, contactDetails: Option[ContactDetails] = None)

object AgentSession {
  implicit val format: OFormat[AgentSession] = Json.format[AgentSession]
}
