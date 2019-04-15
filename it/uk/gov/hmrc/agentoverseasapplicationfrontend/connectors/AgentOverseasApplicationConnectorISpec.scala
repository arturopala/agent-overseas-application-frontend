package uk.gov.hmrc.agentoverseasapplicationfrontend.connectors

import java.net.URL

import com.kenshoo.play.metrics.Metrics
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.FileUploadStatus
import uk.gov.hmrc.agentoverseasapplicationfrontend.stubs.AgentOverseasApplicationStubs
import uk.gov.hmrc.agentoverseasapplicationfrontend.support.BaseISpec
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, HttpPost}


class AgentOverseasApplicationConnectorISpec extends BaseISpec with AgentOverseasApplicationStubs {

  private lazy val metrics = app.injector.instanceOf[Metrics]
  private lazy val http = app.injector.instanceOf[HttpGet with HttpPost]

  private implicit val hc = HeaderCarrier()

  private lazy val connector: AgentOverseasApplicationConnector =
    new AgentOverseasApplicationConnector(new URL(s"http://localhost:$wireMockPort"), http, metrics)

  "createOverseasApplication" should {

    "create an application successfully" in {
      givenPostOverseasApplication(201)

      await(connector.createOverseasApplication(defaultCreateApplicationRequest)) shouldBe (())
    }

    "return exception" when {
      "the application already exists" in {
        givenPostOverseasApplication(409)

        an[Exception] should be thrownBy (await(connector.createOverseasApplication(defaultCreateApplicationRequest)))
      }

      "service is unavailable" in {
        givenPostOverseasApplication(503)

        an[Exception] should be thrownBy (await(connector.createOverseasApplication(defaultCreateApplicationRequest)))
      }
    }
  }

  "upscanPollStatus" should {

    "return a FileUploadStatus with READY status when the file was received from AWS/Upscan" in {
      given200UpscanPollStatusReady()

      await(connector.upscanPollStatus("reference")) shouldBe FileUploadStatus("reference","READY",Some("some"))
    }

    "return a FileUploadStatus with NOT_READY status when the file was NOT received from AWS/Upscan" in {
      given200UpscanPollStatusNotReady()

      await(connector.upscanPollStatus("reference")) shouldBe FileUploadStatus("reference","NOT_READY", None)
    }

    "service is unavailable" in {
      given500UpscanPollStatus()

      an[Exception] should be thrownBy (await(connector.upscanPollStatus("reference")))
    }

  }
}
