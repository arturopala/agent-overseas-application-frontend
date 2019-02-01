package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers

import java.time.{Clock, LocalDate}

import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentoverseasapplicationfrontend.stubs._
import uk.gov.hmrc.agentoverseasapplicationfrontend.support.BaseISpec
import uk.gov.hmrc.http.HeaderCarrier

class StartControllerISpec extends BaseISpec with AgentOverseasApplicationStubs {

  private lazy val controller = app.injector.instanceOf[StartController]
  implicit val hc = HeaderCarrier()

  "GET /not-agent" should {
    "display the non-agent  page when the current user is logged in" in {
      val result = await(controller.showNotAgent(basicRequest(FakeRequest())))

      status(result) shouldBe 200
      result should containLink("button.register", routes.SignOutController.signOutWithContinueUrl().url)
      result should containMessages("nonAgent.title", "nonAgent.p2", "nonAgent.l1", "nonAgent.l2")
      result should containSubstrings(htmlMessage("nonAgent.p1", routes.SignOutController.signOut().url))
    }
  }

  "GET / " should {
    "simply redirect to start of journey showAntiMoneyLaunderingForm" in {
      given200GetOverseasApplications(true)
      val result = await(controller.root(basicRequest(FakeRequest())))

      status(result) shouldBe 303
      redirectLocation(result).get shouldBe routes.ApplicationController.showAntiMoneyLaunderingForm().url
    }
  }

  "/application-status applicationStatus PENDING status" should {
    "200 display content with application creation date & default 0 days if beyond 28 days from creation date" in {
      given200OverseasPendingApplication(Some("2018-02-01"))

      val result = await(controller.applicationStatus(basicRequest(FakeRequest())))

      status(result) shouldBe 200
      result should containSubstrings(htmlMessage("application_not_ready.p1", "Testing Agency", "1 February 2018"),
        htmlMessage("application_not_ready.p3", 0))
      result should containMessages("application_not_ready.title",
        "application_not_ready.h2",
        "application_not_ready.p2",
        "application_not_ready.p4",
        "application_not_ready.h3",
        "application_not_ready.p5")
    }

    "200 28 days to review when fresh application" in {
      given200OverseasPendingApplication(Some(LocalDate.now(Clock.systemUTC()).toString))

      val result = await(controller.applicationStatus(basicRequest(FakeRequest())))

      result should containSubstrings(htmlMessage("application_not_ready.p3", 28))
    }

    "RuntimeException when Pending application not found" in {
      given404OverseasApplications

      an[RuntimeException] shouldBe thrownBy(await(controller.applicationStatus(basicRequest(FakeRequest()))))
    }

    "RuntimeException when unexpected application status" in {
      given200OverseasAcceptedApplication

      an[RuntimeException] shouldBe thrownBy(await(controller.applicationStatus(basicRequest(FakeRequest()))))
    }
  }

  "GET /application-status applicationStatus Rejected status" should {
    "200 show detail about last rejected application with link to start new application" in {
      given200GetOverseasApplications(true)
      val result = await(controller.applicationStatus(basicRequest(FakeRequest())))

      val stubMatchingTradingName = "Tradingname"
      val stubMatchingEmail = "email@domain.com"

      status(result) shouldBe 200
      result should containMessages("statusRejected.title", "statusRejected.heading", "statusRejected.para3")
      result should containSubstrings(htmlEscapedMessage("statusRejected.para1", stubMatchingTradingName),
        htmlMessage("statusRejected.para2", s"<strong class=bold-small>$stubMatchingEmail</strong>"))

      result should containLink("statusRejected.link.text", routes.ApplicationController.showAntiMoneyLaunderingForm().url)
    }

    "RuntimeException, was initialised correctly, however no application to support this and provide data was found" in {
      given404OverseasApplications
      an[RuntimeException] shouldBe thrownBy(await(controller.applicationStatus(basicRequest(FakeRequest()))))
    }

    "RuntimeException when unexpected application status" in {
      given200OverseasAcceptedApplication

      an[RuntimeException] shouldBe thrownBy(await(controller.applicationStatus(basicRequest(FakeRequest()))))
    }
  }
}