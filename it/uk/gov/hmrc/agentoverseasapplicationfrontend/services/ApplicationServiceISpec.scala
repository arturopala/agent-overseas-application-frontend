package uk.gov.hmrc.agentoverseasapplicationfrontend.services

import java.time.LocalDate

import uk.gov.hmrc.agentoverseasapplicationfrontend.models.ApplicationEntityDetails
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.ApplicationStatus.Pending
import uk.gov.hmrc.agentoverseasapplicationfrontend.stubs.AgentOverseasApplicationStubs
import uk.gov.hmrc.agentoverseasapplicationfrontend.support.BaseISpec
import uk.gov.hmrc.http.HeaderCarrier

class ApplicationServiceISpec extends BaseISpec with AgentOverseasApplicationStubs {
  implicit val hc = HeaderCarrier()
  val service = app.injector.instanceOf[ApplicationService]

  "getCurrentApplication" should {
    "return application record for an auth provider id" in {
      given200OverseasPendingApplication()
      await(service.getCurrentApplication) shouldBe Some(ApplicationEntityDetails( LocalDate.parse("2019-01-18"),
        Pending,
        "Testing Agency",
        "test@test.com",
        None))
    }

    "return active application record that will be the most recently made for the auth provider id" in {
      given200GetOverseasApplications(allRejected = false)
      val app = await(service.getCurrentApplication)
      app shouldBe Some(ApplicationEntityDetails( LocalDate.parse("2019-01-22"),
        Pending,
        "Tradingname",
        "email@domain.com",
        None))
    }

    "return empty results for an auth provider id" in {
      given404OverseasApplications
      await(service.getCurrentApplication) shouldBe None
    }
  }
}
