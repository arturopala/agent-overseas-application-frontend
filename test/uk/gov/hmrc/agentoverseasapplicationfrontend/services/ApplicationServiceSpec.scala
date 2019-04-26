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

package uk.gov.hmrc.agentoverseasapplicationfrontend.services

import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito.when
import org.mockito.ArgumentMatchers.{any, eq => eqs}
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.agentoverseasapplicationfrontend.connectors.AgentOverseasApplicationConnector
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.PersonalDetails.RadioOption
import uk.gov.hmrc.agentoverseasapplicationfrontend.models._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.collection.immutable.SortedSet
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class ApplicationServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  val connector = mock[AgentOverseasApplicationConnector]

  val service = new ApplicationService(connector)

  private implicit val hc = HeaderCarrier()

  private val contactDetails = ContactDetails("test", "last", "senior agent", "12345", "test@email.com")
  private val amlsDetails = AmlsDetails("Keogh Chartered Accountants", Some("123456"))
  private val mainBusinessAddress = MainBusinessAddress("line 1", "line 2", None, None, countryCode = "IE")
  private val personalDetails = PersonalDetails(Some(RadioOption.NinoChoice), Some(Nino("AB123456A")), None)
  private val agentCodes = AgentCodes(Some(SaAgentCode("SA123456")), Some(CtAgentCode("CT123456")))

  private val crn = CompanyRegistrationNumber(Some(true), Some(Crn("123456")))

  private val uploadStatus: Option[FileUploadStatus] =
    Some(FileUploadStatus(reference = "fileRef", fileStatus = "READY", fileName = Some("fileName")))

  private val agentSession = AgentSession(
    amlsDetails = Some(amlsDetails),
    contactDetails = Some(contactDetails),
    tradingName = Some("Trading name"),
    mainBusinessAddress = Some(mainBusinessAddress),
    registeredWithHmrc = Some(Yes),
    agentCodes = Some(agentCodes),
    registeredForUkTax = Some(No),
    personalDetails = Some(personalDetails),
    companyRegistrationNumber = Some(crn),
    hasTaxRegNumbers = Some(true),
    taxRegistrationNumbers = Some(SortedSet(Trn("123"), Trn("456"))),
    amlsUploadStatus = uploadStatus,
    trnUploadStatus = uploadStatus,
    tradingAddressUploadStatus = uploadStatus,
    changingAnswers = false
  )

  "ApplicationService" should {

    "create new application" in {
      when(
        connector
          .createOverseasApplication(any[CreateApplicationRequest])(eqs(hc), any[ExecutionContext]))
        .thenReturn(Future.successful(()))

      await(service.createApplication(agentSession)) shouldBe (())
    }

    "returns exception when fails to create new application" in {
      when(
        connector
          .createOverseasApplication(any[CreateApplicationRequest])(eqs(hc), any[ExecutionContext]))
        .thenReturn(Future.failed(new Exception("Something went wrong! Please try again later.")))

      an[Exception] should be thrownBy (await(service.createApplication(agentSession)))
    }
  }

  "poll upscan status" in {
    when(
      connector.upscanPollStatus(any[String])(eqs(hc), any[ExecutionContext])
    ).thenReturn(Future.successful(FileUploadStatus("some", "some", Some("some"))))

    await(service.upscanPollStatus("some")) shouldBe FileUploadStatus("some", "some", Some("some"))
  }

}
