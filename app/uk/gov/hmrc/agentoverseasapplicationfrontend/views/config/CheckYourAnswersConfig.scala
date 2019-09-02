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

package uk.gov.hmrc.agentoverseasapplicationfrontend.views.config

import play.api.i18n.Messages
import play.api.mvc.Call
import uk.gov.hmrc.agentoverseasapplicationfrontend.controllers.routes
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.{AgentCodes, AgentSession, AmlsDetails, ContactDetails, FileUploadStatus, MainBusinessAddress, Yes, YesNo}

case class CheckYourAnswersConfig(
  amlsSection: Option[Section],
  contactDetailsSection: Option[Section],
  businessDetailsSection: Option[Section],
  otherDetailsSection: Option[Section])

object CheckYourAnswersConfig {

  def apply(agentSession: AgentSession, countryName: String)(implicit messages: Messages): CheckYourAnswersConfig =
    CheckYourAnswersConfig(
      amlsSection =
        agentSession.amlsRequired.map(amlsRequired => makeAmlsSection(amlsRequired, agentSession.amlsDetails)),
      contactDetailsSection = agentSession.contactDetails.map(cd => makeContactDetailsSection(cd)),
      businessDetailsSection = {
        for {
          name                       <- agentSession.tradingName
          address                    <- agentSession.mainBusinessAddress
          tradingAddressUploadStatus <- agentSession.tradingAddressUploadStatus
          tradingAddressFileName     <- tradingAddressUploadStatus.fileName
        } yield makeBusinessDetailsSection(name, address, countryName, tradingAddressFileName)
      },
      otherDetailsSection = {
        for {
          isRegisteredWithHmrc <- agentSession.registeredWithHmrc
          trnUploadStatus      <- agentSession.trnUploadStatus
          trnFileName          <- trnUploadStatus.fileName
        } yield makeOtherDetailsSection(isRegisteredWithHmrc, agentSession.agentCodes, trnFileName)
      }
    )

  def makeAmlsSection(amlsRequired: Boolean, amlsDetails: Option[AmlsDetails])(implicit messages: Messages): Section =
    Section(
      Messages("checkAnswers.amlsDetails.title"),
      Seq(
        Some(
          Row(
            Seq(HeadingAndData(Messages("checkAnswers.amlsDetails.required"), amlsRequired.toString)),
            routes.AntiMoneyLaunderingController.showMoneyLaunderingRequired(),
            Seq("check-answers-amls-required")
          )),
        amlsDetails.map(
          details =>
            Row(
              Seq(
                HeadingAndData(Messages("checkAnswers.amlsDetails.supervisoryBody"), details.supervisoryBody),
                HeadingAndData(
                  Messages("checkAnswers.amlsDetails.membershipNumber"),
                  details.membershipNumber.getOrElse(""))
              ),
              routes.ChangingAnswersController.changeAmlsDetails(),
              Seq.empty ++ (if (details.supervisoryBody.nonEmpty) Seq("check-answers-supervisory-body") else Seq.empty)
                ++ (if (details.membershipNumber.nonEmpty) Seq("check-answers-membership-number") else Seq.empty)
          ))
      )
    )

  def makeContactDetailsSection(contactDetails: ContactDetails)(implicit messages: Messages) =
    Section(
      Messages("checkAnswers.contactDetails.title"),
      Seq(
        Some(Row(
          Seq(
            HeadingAndData(
              Messages("checkAnswers.contactDetails.name"),
              s"${contactDetails.firstName} ${contactDetails.lastName}"),
            HeadingAndData(Messages("checkAnswers.contactDetails.jobTitle"), contactDetails.jobTitle),
            HeadingAndData(Messages("checkAnswers.contactDetails.businessTelephone"), contactDetails.businessTelephone),
            HeadingAndData(Messages("checkAnswers.contactDetails.businessEmail"), contactDetails.businessEmail)
          ),
          routes.ChangingAnswersController.changeContactDetails(),
          Seq.empty ++ (if (contactDetails.firstName.nonEmpty) Seq("check-answers-first-name") else Seq.empty) ++
            (if (contactDetails.lastName.nonEmpty) Seq("check-answers-last-name") else Seq.empty) ++
            (if (contactDetails.jobTitle.nonEmpty) Seq("check-answers-job-title") else Seq.empty) ++
            (if (contactDetails.businessTelephone.nonEmpty) Seq("check-answers-business-telephone") else Seq.empty) ++
            (if (contactDetails.businessEmail.nonEmpty) Seq("check-answers-business-email") else Seq.empty)
        )))
    )

  def makeBusinessDetailsSection(
    tradingName: String,
    businessAddress: MainBusinessAddress,
    countryName: String,
    tradingAddressFileName: String)(implicit messages: Messages) = {

    def formatFileName(fileName: String) =
      if (fileName.length > 20) s"${fileName.take(10)}...${fileName.takeRight(10)}"
      else fileName

    Section(
      Messages("checkAnswers.BusinessDetails.title"),
      Seq(
        Some(
          Row(
            Seq(
              HeadingAndData(Messages("checkAnswers.tradingName.title"), tradingName)
            ),
            routes.ChangingAnswersController.changeTradingName(),
            Seq.empty ++ (if (tradingName.nonEmpty) Seq("check-answers-trading-name") else Seq.empty)
          )),
        Some(
          Row(
            Seq(
              HeadingAndData(
                Messages("checkAnswers.mainBusinessAddress.title"),
                s"${businessAddress.addressLine1}\n${businessAddress.addressLine2}\n${businessAddress.addressLine3}\n${businessAddress.addressLine4}\n$countryName"
              )
            ),
            routes.ChangingAnswersController.changeTradingAddress(),
            Seq.empty ++ (if (businessAddress.addressLine1.nonEmpty) Seq("check-answers-address-line1") else Seq.empty) ++
              (if (businessAddress.addressLine2.nonEmpty) Seq("check-answers-address-line2") else Seq.empty) ++
              (if (businessAddress.addressLine3.nonEmpty) Seq("check-answers-address-line3") else Seq.empty) ++
              (if (businessAddress.addressLine4.nonEmpty) Seq("check-answers-address-line4") else Seq.empty) ++
              (if (countryName.nonEmpty) Seq("check-answers-country-name") else Seq.empty)
          )),
        Some(
          Row(
            Seq(
              HeadingAndData(
                Messages("checkAnswers.tradingAddressFile.title"),
                formatFileName(tradingAddressFileName))),
            routes.ChangingAnswersController.changeTradingAddressFile(),
            Seq.empty ++ (if (tradingAddressFileName.nonEmpty) Seq("check-answers-trading-address-file-name")
                          else Seq.empty)
          ))
      )
    )
  }

  def makeOtherDetailsSection(isRegisteredWithHmrc: YesNo, agentCodes: Option[AgentCodes], trnFileName: String)(
    implicit messages: Messages) =
    Section(
      Messages("checkAnswers.OtherBusinessDetails.title"),
      Seq(
        Some(
          Row(
            Seq(
              HeadingAndData(Messages("checkAnswers.registeredWithHmrc.title"), isRegisteredWithHmrc.toString)
            ),
            routes.ChangingAnswersController.changeRegisteredWithHmrc(),
            Seq.empty ++ (if (isRegisteredWithHmrc == Yes) Seq("check-answers-is-registered") else Seq.empty)
          )),
        agentCodes.map(codes => {
          Row(
            Seq(
              HeadingAndData(
                Messages("checkAnswers.agentCode.selfAssessment"),
                codes.selfAssessment.map(_.value).getOrElse("")),
              HeadingAndData(
                Messages("checkAnswers.agentCode.corporationTax"),
                codes.corporationTax.map(_.value).getOrElse(""))
            ),
            routes.ChangingAnswersController.changeAgentCodes(),
            Seq.empty ++ (if (codes.selfAssessment.nonEmpty) Seq("check-answers-sa") else Seq.empty) ++
              (if (codes.corporationTax.nonEmpty) Seq("check-answers-ct") else Seq.empty)
          )
        }),
        Some(
          Row(
            Seq(
              HeadingAndData(Messages("checkAnswers.taxRegistrationNumbersFile.title"), trnFileName)
            ),
            routes.ChangingAnswersController.changeYourTaxRegistrationNumbersFile(),
            Seq.empty ++ (if (trnFileName.nonEmpty) Seq("check-answers-trn-file-name") else Seq.empty)
          ))
      )
    )
}

case class Section(subtitle: String, rows: Seq[Option[Row]])

case class Row(headingAndData: Seq[HeadingAndData], changeLink: Call, gaEvents: Seq[String])

case class HeadingAndData(heading: String, data: String)
