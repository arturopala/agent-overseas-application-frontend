package uk.gov.hmrc.agentoverseasapplicationfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.Json
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.{ApplicationStatus, CreateApplicationRequest}

trait AgentOverseasApplicationStubs {
  val allStatuses = ApplicationStatus.allStatuses.map(status => s"statusIdentifier=${status.key}").mkString("&")

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
       |      "regNo": "1234",
       |      "utr": "4000000009",
       |      "nino": "AB123456A",
       |      "taxRegNo": ["1234567"]
       |    }
       |  }
       |}
     """.stripMargin

  val defaultCreateApplicationRequest: CreateApplicationRequest = Json.parse(defaultRequestBody).as[CreateApplicationRequest]

  def given200OverseasPendingApplication(appCreateDate: Option[String] = Some("2019-01-18")): Unit = {
    val responseData = StubsTestData.pendingApplication(appCreateDate.getOrElse("2019-01-18"))
    stubFor(get(urlEqualTo(s"/agent-overseas-application/application?$allStatuses"))
      .willReturn(aResponse()
        .withBody(responseData)
        .withStatus(200))
    )
  }

  def given200OverseasAcceptedApplication(): Unit = {
    val responseData = StubsTestData.acceptedApplication
    stubFor(get(urlEqualTo(s"/agent-overseas-application/application?$allStatuses"))
      .willReturn(aResponse()
        .withBody(responseData)
        .withStatus(200))
    )
  }

  def given200OverseasRedirectStatusApplication(redirectStatus: String): Unit = {
    val responseData = StubsTestData.applicationInRedirectStatus(redirectStatus)
    stubFor(get(urlEqualTo(s"/agent-overseas-application/application?$allStatuses"))
    .willReturn(aResponse()
    .withBody(responseData)
    .withStatus(200)))
  }

  def given200GetOverseasApplications(allRejected: Boolean): Unit = {
    val requestBody = if (allRejected) StubsTestData.allRejected else StubsTestData.notAllRejected
    stubFor(get(urlEqualTo(s"/agent-overseas-application/application?$allStatuses"))
      .willReturn(aResponse()
        .withBody(requestBody)
        .withStatus(200))
    )
  }

  def given404OverseasApplications(): Unit = {
    stubFor(get(urlEqualTo(s"/agent-overseas-application/application?$allStatuses"))
      .willReturn(aResponse()
        .withStatus(404)))
  }

  def given500GetOverseasApplication(): Unit = {
    stubFor(get(urlEqualTo(s"/agent-overseas-application/application?$allStatuses"))
      .willReturn(aResponse()
        .withStatus(500)))
  }
}
