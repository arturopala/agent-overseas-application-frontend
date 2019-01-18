package uk.gov.hmrc.agentoverseasapplicationfrontend.models

import play.api.libs.json.{Json, OFormat}

import scala.collection.immutable.SortedSet

case class AgentSession(
  amlsDetails: Option[AmlsDetails] = None,
  contactDetails: Option[ContactDetails] = None,
  tradingName: Option[String] = None,
  mainBusinessAddress: Option[MainBusinessAddress] = None,
  registeredWithHmrc: Option[YesNo] = None,
  agentCodes: Option[AgentCodes] = None,
  registeredForUkTax: Option[YesNo] = None,
  personalDetails: Option[PersonalDetails] = None,
  companyRegistrationNumber: Option[CompanyRegistrationNumber] = None,
  hasTaxRegNumbers: Option[Boolean] = None,
  taxRegistrationNumbers: Option[SortedSet[String]] = None,
  changingAnswers: Boolean = false) {

  def sanitize: AgentSession = {
    val agentCodes = if (this.registeredWithHmrc.contains(Yes)) this.agentCodes else None

    val registeredForUkTax = this.registeredWithHmrc match {
      case Some(Yes) if !this.agentCodes.exists(_.hasOneOrMoreCodes) => this.registeredForUkTax
      case Some(No | Unsure)                                         => this.registeredForUkTax
      case _                                                         => None
    }
    val personalDetails = if (registeredForUkTax.contains(Yes)) this.personalDetails else None
    val companyRegistrationNumber = registeredForUkTax.flatMap(_ => this.companyRegistrationNumber)
    val taxRegistrationNumbers = registeredForUkTax.flatMap(_ => this.taxRegistrationNumbers)

    AgentSession(
      this.amlsDetails,
      this.contactDetails,
      this.tradingName,
      this.mainBusinessAddress,
      this.registeredWithHmrc,
      agentCodes,
      registeredForUkTax,
      personalDetails,
      companyRegistrationNumber,
      this.hasTaxRegNumbers,
      taxRegistrationNumbers,
      this.changingAnswers
    )
  }
}

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
    def unapply(session: Option[AgentSession]): Option[YesNo] = session.flatMap(_.registeredWithHmrc)
  }

  object MissingAgentCodes {
    def unapply(session: Option[AgentSession]): Boolean = session.exists(_.agentCodes.isEmpty)
  }

  object HasAnsweredWithOneOrMoreAgentCodes {
    def unapply(session: Option[AgentSession]): Boolean = session.flatMap(_.agentCodes).exists(_.hasOneOrMoreCodes)
  }

  object HasAnsweredWithNoAgentCodes {
    def unapply(session: Option[AgentSession]): Boolean =
      session.flatMap(_.agentCodes).exists(_.hasOneOrMoreCodes == false)
  }

  object MissingRegisteredForUkTax {
    def unapply(session: Option[AgentSession]): Boolean = session.exists(_.registeredForUkTax.isEmpty)
  }

  object IsRegisteredForUkTax {
    def unapply(session: Option[AgentSession]): Option[YesNo] = session.flatMap(_.registeredForUkTax)
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
