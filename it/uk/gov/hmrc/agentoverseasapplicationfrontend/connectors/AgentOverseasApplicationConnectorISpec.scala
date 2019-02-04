package uk.gov.hmrc.agentoverseasapplicationfrontend.connectors

import java.net.URL
import java.time.LocalDate

import com.kenshoo.play.metrics.Metrics
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.{ApplicationEntityDetails, ApplicationStatus}
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

  "rejectedApplication" should {

    "return the most recently reviewed Application when there are several applications returned from the BE all in rejected status" in {
      given200GetOverseasApplications(true)

      await(connector.rejectedApplication) shouldBe Some(
        ApplicationEntityDetails(
          applicationCreationDate = LocalDate.parse("2019-01-21"),
          ApplicationStatus("rejected"),
          "Tradingname",
          "email@domain.com",
          Some(LocalDate.parse("2019-01-22"))
      ))
    }

    "return None when there are several applications returned from the BE and not all are in rejected status" in {
      given200GetOverseasApplications(false)

      await(connector.rejectedApplication) shouldBe None
    }

    "return None when no applications were found in the BE" in {
      given404OverseasApplications()

      await(connector.rejectedApplication) shouldBe None
    }

    "An exception should be thrown when there is a problem with the BE server" in {
      given500GetOverseasApplication()

      an[Exception] should be thrownBy await(connector.rejectedApplication)
    }
  }
}
