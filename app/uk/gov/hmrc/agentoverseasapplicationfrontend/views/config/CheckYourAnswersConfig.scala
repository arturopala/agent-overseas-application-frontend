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
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.AmlsDetails

object CheckYourAnswersConfig {

  def makeAmlsSection(amlsRequired: Boolean, amlsDetails: AmlsDetails)(
    implicit messages: Messages) =
    Section(
      Messages("checkAnswers.amlsDetails.title"),
      Seq(
        Row(
          Seq(HeadingAndData(Messages("checkAnswers.amlsDetails.required"), amlsRequired.toString)),
          routes.AntiMoneyLaunderingController.showMoneyLaunderingRequired()
        ),
        Row(
          Seq(
            HeadingAndData(Messages("checkAnswers.amlsDetails.supervisoryBody"), amlsDetails.supervisoryBody),
            HeadingAndData(
              Messages("checkAnswers.amlsDetails.membershipNumber"),
              amlsDetails.membershipNumber.getOrElse(""))
          ),
          routes.ChangingAnswersController.changeAmlsDetails()
        )
      ),
      Seq.empty ++ (if(amlsDetails.supervisoryBody.nonEmpty) Seq("check-answers-supervisory-body") else Seq.empty)
      ++(if(amlsDetails.membershipNumber.nonEmpty) Seq("check-answers-membership-number") else Seq.empty)
    )

}

case class Section(subtitle: String, rows: Seq[Row], gaEvents: Seq[String])

case class Row(headingAndData: Seq[HeadingAndData], changeLink: Call)

case class HeadingAndData(heading: String, data: String)
