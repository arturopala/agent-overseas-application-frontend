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
import play.api.libs.functional.syntax._
import uk.gov.hmrc.domain.{Nino, SaUtr}

case class CreateApplicationRequest(
  amlsRequired: Boolean,
  amls: Option[AmlsDetails],
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
  taxRegNo: Option[Seq[Trn]],
  amlsFileRef: String,
  tradingAddressFileRef: String,
  taxRegFileRef: Option[String])

object CreateApplicationRequest {

  def apply(agentSession: AgentSession): CreateApplicationRequest =
    (for {
      amlsRequired <- agentSession.amlsRequired
      contactDetails          <- agentSession.contactDetails
      tradingName             <- agentSession.tradingName
      businessAddress         <- agentSession.mainBusinessAddress
      isHmrcAgentRegistered   <- agentSession.registeredWithHmrc
      amlsFileRef             <- agentSession.amlsUploadStatus.map(_.reference)
      tradingAddressFileRef   <- agentSession.tradingAddressUploadStatus.map(_.reference)
    } yield
      CreateApplicationRequest(
        amlsRequired,
        agentSession.amlsDetails,
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
        agentSession.taxRegistrationNumbers.map(_.toSeq),
        amlsFileRef,
        tradingAddressFileRef,
        agentSession.trnUploadStatus.map(_.reference)
      )).getOrElse(throw new Exception("Could not create application request from agent session"))

  implicit val writes: Writes[CreateApplicationRequest] = (
    (__ \ "amlsRequired").write[Boolean] and
    (__ \ "amls" \ "supervisoryBody").write[Option[String]] and
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
      (__ \ "tradingDetails" \ "taxRegistrationNumbers").write[Option[Seq[Trn]]] and
      (__ \ "amlsFileRef").write[String] and
      (__ \ "tradingAddressFileRef").write[String] and
      (__ \ "taxRegFileRef").write[Option[String]]
  ) { request: CreateApplicationRequest =>
    (
    request.amlsRequired,
      request.amls.map(_.supervisoryBody),
      request.amls.flatMap(_.membershipNumber),
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
      request.taxRegNo,
      request.amlsFileRef,
      request.tradingAddressFileRef,
    request.taxRegFileRef)
  }

  implicit val reads: Reads[CreateApplicationRequest] = (
    (__ \ "amlsRequired").read[Boolean] and
    (__ \ "amls" \ "supervisoryBody").readNullable[String] and
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
      (__ \ "tradingDetails" \ "taxRegistrationNumbers").readNullable[Seq[Trn]] and
      (__ \ "amlsFileRef").read[String] and
      (__ \ "tradingAddressFileRef").read[String] and
      (__ \ "taxRegFileRef").readNullable[String]
    ) ((
    amlsRequired,
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
      taxRegNo,
      amlsFileRef,
      tradingAddressFileRef,
      taxRegFileRef) => CreateApplicationRequest(amlsRequired, supervisoryBody.map(s => AmlsDetails(s, membershipNumber)),
    contactDetails, tradingName, businessAddress,
    isUkRegisteredTaxOrNino, isHmrcAgentRegistered,
    saAgentCode, ctAgentCode, regNo, utr, nino, taxRegNo, amlsFileRef,tradingAddressFileRef,taxRegFileRef))

}
