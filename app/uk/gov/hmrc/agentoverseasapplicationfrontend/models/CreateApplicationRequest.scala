package uk.gov.hmrc.agentoverseasapplicationfrontend.models

import play.api.libs.json._
import play.api.libs.functional.syntax._
import uk.gov.hmrc.domain.{Nino, SaUtr}

case class CreateApplicationRequest(
  amls: AmlsDetails,
  contactDetails: ContactDetails,
  tradingName: String,
  businessAddress: MainBusinessAddress,
  isUkRegisteredTaxOrNino: Option[YesNoUnsure],
  isHmrcAgentRegistered: YesNoUnsure,
  saAgentCode: Option[String],
  ctAgentCode: Option[String],
  vatAgentCode: Option[String],
  payeAgentCode: Option[String],
  regNo: Option[String],
  utr: Option[SaUtr],
  nino: Option[Nino],
  taxRegNo: Option[Seq[String]])

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
        agentSession.agentCodes.flatMap(_.vat),
        agentSession.agentCodes.flatMap(_.paye),
        agentSession.companyRegistrationNumber.flatMap(_.registrationNumber),
        agentSession.personalDetails.flatMap(_.saUtr),
        agentSession.personalDetails.flatMap(_.nino),
        agentSession.taxRegistrationNumbers.map(_.toSeq)
      )).getOrElse(throw new Exception("Could not create application request from agent session"))

  implicit val writes: Writes[CreateApplicationRequest] = (
    (__ \ "amls" \ "supervisoryBody").write[String] and
      (__ \ "amls" \ "supervisionMemberId").write[Option[String]] and
      (__ \ "contactDetails").write[ContactDetails] and
      (__ \ "businessDetail" \ "tradingName").write[String] and
      (__ \ "businessDetail" \ "businessAddress").write[MainBusinessAddress] and
      (__ \ "businessDetail" \ "extraInfo" \ "isUkRegisteredTaxOrNino").write[Option[YesNoUnsure]] and
      (__ \ "businessDetail" \ "extraInfo" \ "isHmrcAgentRegistered").write[YesNoUnsure] and
      (__ \ "businessDetail" \ "extraInfo" \ "saAgentCode").write[Option[String]] and
      (__ \ "businessDetail" \ "extraInfo" \ "ctAgentCode").write[Option[String]] and
      (__ \ "businessDetail" \ "extraInfo" \ "vatAgentCode").write[Option[String]] and
      (__ \ "businessDetail" \ "extraInfo" \ "payeAgentCode").write[Option[String]] and
      (__ \ "businessDetail" \ "extraInfo" \ "regNo").write[Option[String]] and
      (__ \ "businessDetail" \ "extraInfo" \ "utr").write[Option[SaUtr]] and
      (__ \ "businessDetail" \ "extraInfo" \ "nino").write[Option[Nino]] and
      (__ \ "businessDetail" \ "extraInfo" \ "taxRegNo").write[Option[Seq[String]]]
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
      request.vatAgentCode,
      request.payeAgentCode,
      request.regNo,
      request.utr,
      request.nino,
      request.taxRegNo)
  }

  implicit val reads: Reads[CreateApplicationRequest] = (
    (__ \ "amls" \ "supervisoryBody").read[String] and
      (__ \ "amls" \ "supervisionMemberId").readNullable[String] and
      (__ \ "contactDetails").read[ContactDetails] and
      (__ \ "businessDetail" \ "tradingName").read[String] and
      (__ \ "businessDetail" \ "businessAddress").read[MainBusinessAddress] and
      (__ \ "businessDetail" \ "extraInfo" \ "isUkRegisteredTaxOrNino").readNullable[YesNoUnsure] and
      (__ \ "businessDetail" \ "extraInfo" \ "isHmrcAgentRegistered").read[YesNoUnsure] and
      (__ \ "businessDetail" \ "extraInfo" \ "saAgentCode").readNullable[String] and
      (__ \ "businessDetail" \ "extraInfo" \ "ctAgentCode").readNullable[String] and
      (__ \ "businessDetail" \ "extraInfo" \ "vatAgentCode").readNullable[String] and
      (__ \ "businessDetail" \ "extraInfo" \ "payeAgentCode").readNullable[String] and
      (__ \ "businessDetail" \ "extraInfo" \ "regNo").readNullable[String] and
      (__ \ "businessDetail" \ "extraInfo" \ "utr").readNullable[SaUtr] and
      (__ \ "businessDetail" \ "extraInfo" \ "nino").readNullable[Nino] and
      (__ \ "businessDetail" \ "extraInfo" \ "taxRegNo").readNullable[Seq[String]]
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
      vatAgentCode,
      payeAgentCode,
      regNo,
      utr,
      nino,
      taxRegNo) => CreateApplicationRequest(AmlsDetails(supervisoryBody, membershipNumber),
    contactDetails, tradingName, businessAddress,
    isUkRegisteredTaxOrNino, isHmrcAgentRegistered,
    saAgentCode, ctAgentCode, vatAgentCode, payeAgentCode,
    regNo, utr, nino, taxRegNo))

}
