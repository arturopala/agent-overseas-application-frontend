package uk.gov.hmrc.agentoverseasapplicationfrontend.models

import play.api.libs.json._
import play.api.libs.functional.syntax._
import uk.gov.hmrc.domain.{Nino, SaUtr}

case class CreateApplicationRequest(
  amls: AmlsDetails,
  contactDetails: ContactDetails,
  tradingName: String,
  businessAddress: MainBusinessAddress,
  isUkRegisteredTaxOrNino: Option[YesNo],
  isHmrcAgentRegistered: YesNo,
  saAgentCode: Option[SaAgentCode],
  ctAgentCode: Option[CtAgentCode],
  regNo: Option[Crn],
  utr: Option[SaUtr],
  nino: Option[Nino],
  taxRegNo: Option[Seq[Trn]])

object CreateApplicationRequest {

  def apply(agentSession: AgentSession): CreateApplicationRequest =
    (for {
      amls                    <- agentSession.amlsDetails
      contactDetails          <- agentSession.contactDetails
      tradingName             <- agentSession.tradingName
      businessAddress         <- agentSession.mainBusinessAddress
      isHmrcAgentRegistered   <- agentSession.registeredWithHmrc
    } yield
      CreateApplicationRequest(
        amls,
        contactDetails,
        tradingName,
        businessAddress,
        agentSession.registeredForUkTax,
        isHmrcAgentRegistered,
        agentSession.agentCodes.flatMap(_.selfAssessment),
        agentSession.agentCodes.flatMap(_.corporationTax),
        agentSession.companyRegistrationNumber.flatMap(_.registrationNumber),
        agentSession.personalDetails.flatMap(_.saUtr),
        agentSession.personalDetails.flatMap(_.nino),
        agentSession.taxRegistrationNumbers.map(_.toSeq)
      )).getOrElse(throw new Exception("Could not create application request from agent session"))

  implicit val writes: Writes[CreateApplicationRequest] = (
    (__ \ "amls" \ "supervisoryBody").write[String] and
      (__ \ "amls" \ "membershipNumber").write[Option[String]] and
      (__ \ "contactDetails").write[ContactDetails] and
      (__ \ "tradingDetails" \ "tradingName").write[String] and
      (__ \ "tradingDetails" \ "tradingAddress").write[MainBusinessAddress] and
      (__ \ "tradingDetails" \ "isUkRegisteredTaxOrNino").write[Option[YesNo]] and
      (__ \ "tradingDetails" \ "isHmrcAgentRegistered").write[YesNo] and
      (__ \ "tradingDetails" \ "saAgentCode").write[Option[SaAgentCode]] and
      (__ \ "tradingDetails" \ "ctAgentCode").write[Option[CtAgentCode]] and
      (__ \ "tradingDetails" \ "companyRegistrationNumber").write[Option[Crn]] and
      (__ \ "personalDetails" \ "saUtr").write[Option[SaUtr]] and
      (__ \ "personalDetails" \ "nino").write[Option[Nino]] and
      (__ \ "tradingDetails" \ "taxRegistrationNumbers").write[Option[Seq[Trn]]]
  ) { request: CreateApplicationRequest =>
    (
      request.amls.supervisoryBody,
      request.amls.membershipNumber,
      request.contactDetails,
      request.tradingName,
      request.businessAddress,
      request.isUkRegisteredTaxOrNino,
      request.isHmrcAgentRegistered,
      request.saAgentCode,
      request.ctAgentCode,
      request.regNo,
      request.utr,
      request.nino,
      request.taxRegNo)
  }

  implicit val reads: Reads[CreateApplicationRequest] = (
    (__ \ "amls" \ "supervisoryBody").read[String] and
      (__ \ "amls" \ "membershipNumber").readNullable[String] and
      (__ \ "contactDetails").read[ContactDetails] and
      (__ \ "tradingDetails" \ "tradingName").read[String] and
      (__ \ "tradingDetails" \ "tradingAddress").read[MainBusinessAddress] and
      (__ \ "tradingDetails" \ "isUkRegisteredTaxOrNino").readNullable[YesNo] and
      (__ \ "tradingDetails" \ "isHmrcAgentRegistered").read[YesNo] and
      (__ \ "tradingDetails" \ "saAgentCode").readNullable[SaAgentCode] and
      (__ \ "tradingDetails" \ "ctAgentCode").readNullable[CtAgentCode] and
      (__ \ "tradingDetails" \ "companyRegistrationNumber").readNullable[Crn] and
      (__ \ "personalDetails" \ "saUtr").readNullable[SaUtr] and
      (__ \ "personalDetails" \ "nino").readNullable[Nino] and
      (__ \ "tradingDetails" \ "taxRegistrationNumbers").readNullable[Seq[Trn]]
    ) ((
      supervisoryBody,
      membershipNumber,
      contactDetails,
      tradingName,
      businessAddress,
      isUkRegisteredTaxOrNino,
      isHmrcAgentRegistered,
      saAgentCode,
      ctAgentCode,
      regNo,
      utr,
      nino,
      taxRegNo) => CreateApplicationRequest(AmlsDetails(supervisoryBody, membershipNumber),
    contactDetails, tradingName, businessAddress,
    isUkRegisteredTaxOrNino, isHmrcAgentRegistered,
    saAgentCode, ctAgentCode, regNo, utr, nino, taxRegNo))

}
