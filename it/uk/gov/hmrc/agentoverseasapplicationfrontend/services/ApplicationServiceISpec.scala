package uk.gov.hmrc.agentoverseasapplicationfrontend.services

import java.time.LocalDateTime

import uk.gov.hmrc.agentoverseasapplicationfrontend.models.{ApplicationEntityDetails, ApplicationStatus}
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.ApplicationStatus.Pending
import uk.gov.hmrc.agentoverseasapplicationfrontend.stubs.AgentOverseasApplicationStubs
import uk.gov.hmrc.agentoverseasapplicationfrontend.support.BaseISpec
import uk.gov.hmrc.http.HeaderCarrier

class ApplicationServiceISpec extends BaseISpec with AgentOverseasApplicationStubs {
  implicit val hc = HeaderCarrier()
  val service = app.injector.instanceOf[ApplicationService]

  "getCurrentApplication" should {
    "return application record for an auth provider id" in {
      given200OverseasPendingApplication(Some("2019-02-18T15:11:51.729"))
      await(service.getCurrentApplication) shouldBe Some(ApplicationEntityDetails(LocalDateTime.parse("2019-02-18T15:11:51.729"),
        Pending,
        "Testing Agency",
        "test@test.com",
        None))
    }

    "return active application record that will be the most recently made for the auth provider id" in {
      given200GetOverseasApplications(allRejected = false)
      val app = await(service.getCurrentApplication)
      app shouldBe Some(ApplicationEntityDetails( LocalDateTime.parse("2019-02-20T15:11:51.729"),
        Pending,
        "Testing Agency",
        "test@test.com",
        None))
    }

    "return empty results for an auth provider id" in {
      given404OverseasApplications()
      await(service.getCurrentApplication) shouldBe None
    }
  }

  "rejectedApplication" should {

    "return the most recently reviewed Application when there are several applications returned from the BE all in rejected status" in {
      given200GetOverseasApplications(true)

      await(service.rejectedApplication) shouldBe Some(
        ApplicationEntityDetails(
          applicationCreationDate = LocalDateTime.parse("2019-02-20T15:11:51.729"),
          ApplicationStatus("rejected"),
          "Testing Agency",
          "test@test.com",
          Some(LocalDateTime.parse("2019-02-21T10:35:21.650"))
        ))
    }

    "return None when there are several applications returned from the BE and not all are in rejected status" in {
      given200GetOverseasApplications(false)

      await(service.rejectedApplication) shouldBe None
    }

    "return None when no applications were found in the BE" in {
      given404OverseasApplications()

      await(service.rejectedApplication) shouldBe None
    }

    "An exception should be thrown when there is a problem with the BE server" in {
      given500GetOverseasApplication()

      an[Exception] should be thrownBy await(service.rejectedApplication)
    }
  }
}
