package uk.gov.hmrc.agentoverseasapplicationfrontend.models

import play.api.libs.json.{Json, OFormat}

case class AgentSession(
  amlsDetails: Option[AmlsDetails] = None,
  contactDetails: Option[ContactDetails] = None,
  tradingName: Option[String] = None,
  businessAddress: Option[String] = None)

object AgentSession {
  implicit val format: OFormat[AgentSession] = Json.format[AgentSession]

  object MissingAmlsDetails {
    def unapply(session: Option[AgentSession]): Boolean = session.exists(_.amlsDetails.isEmpty)
  }

  object MissingContactDetails {
    def unapply(session: Option[AgentSession]): Boolean = session.exists(_.contactDetails.isEmpty)
  }

  object MissingTradingName {
    def unapply(session: Option[AgentSession]): Boolean = session.exists(_.tradingName.isEmpty)
  }

  object MissingMainBusinessAddress {
    def unapply(session: Option[AgentSession]): Boolean = session.exists(_.businessAddress.isEmpty)
  }
}
