package uk.gov.hmrc.agentoverseasapplicationfrontend.models

import play.api.libs.json.{Json, OFormat}

case class AgentSession(
  amlsDetails: Option[AmlsDetails] = None,
  contactDetails: Option[ContactDetails] = None,
  tradingName: Option[String] = None,
  mainBusinessAddress: Option[MainBusinessAddress] = None,
  registeredWithHmrc: Option[YesNoUnsure] = None,
  selfAssessmentAgentCode: Option[String] = None,
  registeredForUkTax: Option[YesNoUnsure] = None,
  personalDetails: Option[PersonalDetails] = None,
  companyRegistrationNumber: Option[CompanyRegistrationNumber] = None,
  hasTaxRegNumbers: Option[Boolean] = None,
  taxRegistrationNumbers: Option[Seq[String]] = None)

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

  object MissingTradingAddress {
    def unapply(session: Option[AgentSession]): Boolean = session.exists(_.mainBusinessAddress.isEmpty)
  }

  object MissingRegisteredWithHmrc {
    def unapply(session: Option[AgentSession]): Boolean = session.exists(_.registeredWithHmrc.isEmpty)
  }

  object IsRegisteredWithHmrc {
    def unapply(session: Option[AgentSession]): Option[YesNoUnsure] = session.flatMap(_.registeredWithHmrc)
  }

  object MissingRegisteredForUkTax {
    def unapply(session: Option[AgentSession]): Boolean = session.exists(_.registeredForUkTax.isEmpty)
  }

  object IsRegisteredForUkTax {
    def unapply(session: Option[AgentSession]): Option[YesNoUnsure] = session.flatMap(_.registeredForUkTax)
  }

  object MissingPersonalDetails {
    def unapply(session: Option[AgentSession]): Boolean =
      session.flatMap(_.registeredForUkTax) match {
        case Some(No)     => false
        case Some(Unsure) => false
        case _            => session.exists(_.personalDetails.isEmpty)
      }
  }

  object MissingCompanyRegistrationNumber {
    def unapply(session: Option[AgentSession]): Boolean = session.exists(_.companyRegistrationNumber.isEmpty)
  }

  object MissingHasTaxRegistrationNumber {
    def unapply(session: Option[AgentSession]): Boolean = session.exists(_.hasTaxRegNumbers.isEmpty)
  }

  object HasTaxRegistrationNumber {
    def unapply(session: Option[AgentSession]): Boolean = session.exists(_.hasTaxRegNumbers.getOrElse(false))
  }

  object NoTaxRegistrationNumber {
    def unapply(session: Option[AgentSession]): Boolean =
      session.exists(_.hasTaxRegNumbers.getOrElse(true) == false) //interested in false so getOrElse(true) is the bad case
  }
}
