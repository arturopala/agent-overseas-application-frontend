package uk.gov.hmrc.agentoverseasapplicationfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.Json
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.{ApplicationStatus, CreateApplicationRequest}

trait AgentOverseasApplicationStubs {

  def givenPostOverseasApplication(status: Int, requestBody: String = defaultRequestBody): Unit = {
    stubFor(post(urlEqualTo(s"/agent-overseas-application/application"))
      .withRequestBody(equalToJson(requestBody))
      .willReturn(aResponse()
        .withStatus(status)))
  }

  val defaultRequestBody =
    s"""
       |{
       |  "amls": {
       |    "supervisoryBody": "Association of AccountingTechnicians (AAT)",
       |    "supervisionMemberId": "12121"
       |  },
       |  "contactDetails": {
       |    "firstName": "Bob",
       |    "lastName": "Anderson",
       |    "jobTitle": "Accountant",
       |    "businessTelephone": "123456789",
       |    "businessEmail": "test@example.com"
       |  },
       |  "businessDetail": {
       |    "tradingName": "Some business trading Name",
       |    "businessAddress": {
       |      "addressLine1": "50 SomeStreet",
       |      "addressLine2": "Some town",
       |      "countryCode": "IE"
       |    },
       |    "extraInfo": {
       |      "isUkRegisteredTaxOrNino": {
       |        "str": "yes"
       |      },
       |      "isHmrcAgentRegistered": {
       |        "str": "no"
       |      },
       |      "saAgentCode": "SA123456",
       |      "ctAgentCode": "CT123456",
       |      "vatAgentCode": "VAT123456",
       |      "payeAgentCode": "PAYE123456",
       |      "regNo": "1234",
       |      "utr": "4000000009",
       |      "nino": "AB123456A",
       |      "taxRegNo": ["1234567"]
       |    }
       |  }
       |}
     """.stripMargin

  val defaultCreateApplicationRequest: CreateApplicationRequest = Json.parse(defaultRequestBody).as[CreateApplicationRequest]

  def given200GetOverseasApplications(allRejected: Boolean): Unit = {
    val allStatuses = ApplicationStatus.allStatuses.map(status => s"statusIdentifier=${status.key}").mkString("&")
    val requestBody = if (allRejected) TestData.allRejected else TestData.notAllRejected

    stubFor(get(urlEqualTo(s"/agent-overseas-application/application?$allStatuses"))
      .willReturn(aResponse()
        .withBody(requestBody)
        .withStatus(200))
    )
  }

  def given404GetOverseasApplication: Unit = {
    val allStatuses = ApplicationStatus.allStatuses.map(status => s"statusIdentifier=${status.key}").mkString("&")
    stubFor(get(urlEqualTo(s"/agent-overseas-application/application?$allStatuses"))
      .willReturn(aResponse()
        .withStatus(404)))
  }

  def given500GetOverseasApplication: Unit = {
    val allStatuses = ApplicationStatus.allStatuses.map(status => s"statusIdentifier=${status.key}").mkString("&")
    stubFor(get(urlEqualTo(s"/agent-overseas-application/application?$allStatuses"))
      .willReturn(aResponse()
        .withStatus(500)))
  }
}
