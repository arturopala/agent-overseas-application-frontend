package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers

import play.api.test.FakeRequest
import play.api.test.Helpers.{LOCATION, redirectLocation}
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.{AgentSession, AmlsDetails}
import uk.gov.hmrc.agentoverseasapplicationfrontend.stubs.AgentOverseasApplicationStubs
import uk.gov.hmrc.agentoverseasapplicationfrontend.support.BaseISpec
import uk.gov.hmrc.http.HeaderCarrier

class AntiMoneyLaunderingControllerISpec extends BaseISpec with AgentOverseasApplicationStubs {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private lazy val controller: AntiMoneyLaunderingController = app.injector.instanceOf[AntiMoneyLaunderingController]

  "GET /money-laundering-registration" should {

    "redirect to it self when agentSession not initialised, should only be done once as auth action should initialise agentSession" in {
      given404OverseasApplications()
      val result = await(controller.showMoneyLaunderingRequired(cleanCredsAgent(FakeRequest())))

      redirectLocation(result).get shouldBe routes.AntiMoneyLaunderingController.showMoneyLaunderingRequired().url
      await(sessionStoreService.fetchAgentSession).isDefined shouldBe true
    }

    "display the is money laundering required page" in {
      await(sessionStoreService.cacheAgentSession(AgentSession()))
      val authenticatedRequest = cleanCredsAgent(FakeRequest())

      val result = await(controller.showMoneyLaunderingRequired(authenticatedRequest))
      status(result) shouldBe 200
      result should containSubstrings(
        "Does your country require you to register with a money laundering supervisory body?",
        "Yes",
        "No")
    }
  }

  "POST /money-laundering-registration" should {
    "redirect to /money-laundering when YES is selected" in {
      await(sessionStoreService.cacheAgentSession(AgentSession()))
      val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("amlsRequired" -> "true")

      val result = await(controller.submitMoneyLaunderingRequired(authenticatedRequest))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.AntiMoneyLaunderingController.showAntiMoneyLaunderingForm().url)

      sessionStoreService.fetchAgentSession.get.amlsRequired shouldBe Some(true)
    }

    "redirect to /contact-details when NO is selected" in {
      await(sessionStoreService.cacheAgentSession(AgentSession()))
      val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("amlsRequired" -> "false")

      val result = await(controller.submitMoneyLaunderingRequired(authenticatedRequest))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.ApplicationController.showContactDetailsForm().url)

      sessionStoreService.fetchAgentSession.get.amlsRequired shouldBe Some(false)
    }

    "redisplay the page with errors when no radio button is selected" in {
      await(sessionStoreService.cacheAgentSession(AgentSession()))
      val authenticatedRequest = cleanCredsAgent(FakeRequest())

      val result = await(controller.submitMoneyLaunderingRequired(authenticatedRequest))

      status(result) shouldBe 200
      result should containSubstrings(
        "Does your country require you to register with a money laundering supervisory body?",
        "Yes",
        "No",
        "Select yes if your country requires you to register with a money laundering supervisory body")
    }
  }


  "GET /money-laundering" should {

    "display the money-laundering form" in {
      given404OverseasApplications()
      await(sessionStoreService.cacheAgentSession(AgentSession()))
      val result = await(controller.showAntiMoneyLaunderingForm(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 200

      result should containMessages(
        "amls.title",
        "amls.inset.p1",
        "amls.form.supervisory_body",
        "amls.form.membership_number",
        "amls.hint.expandable",
        "amls.hint.expandable.p1"
      )

      result should containSubstrings(routes.SignOutController.signOut().url)
    }

    "display the money-laundering form with correct back button link when user is CHANGING ANSWERS" in {
      given404OverseasApplications()
      await(sessionStoreService.cacheAgentSession(AgentSession(changingAnswers = true)))
      val authenticatedRequest = cleanCredsAgent(FakeRequest())

      val result = await(controller.showAntiMoneyLaunderingForm(authenticatedRequest))

      status(result) shouldBe 200

      result should containLink("button.back", routes.ApplicationController.showCheckYourAnswers().url)
    }

    "display the money-laundering form with correct back button link when user is not changing answers and Has seen previously rejected application page" in {
      await(sessionStoreService.cacheAgentSession(AgentSession()))
      given200GetOverseasApplications(allRejected = true)

      val result = await(controller.showAntiMoneyLaunderingForm(cleanCredsAgent(FakeRequest())))

      status(result) shouldBe 200

      result should containLink("button.back", routes.StartController.applicationStatus().url)
    }

  }

  "POST /money-laundering" should {
    "redirect to upload/amls" in {
      await(sessionStoreService.cacheAgentSession(AgentSession()))
      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("amlsBody" -> "Association of AccountingTechnicians (AAT)", "membershipNumber" -> "123445")

      val result = await(controller.submitAntiMoneyLaundering(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.FileUploadController.showAmlsUploadForm().url

      val amlsDetails = await(sessionStoreService.fetchAgentSession).get.amlsDetails

      amlsDetails shouldBe Some(AmlsDetails("Association of AccountingTechnicians (AAT)", Some("123445")))
    }

    "redirect to check-your-answers if user is changing the details" in {
      //pre-state
      await(sessionStoreService.cacheAgentSession(AgentSession(changingAnswers = true)))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("amlsBody" -> "Association of AccountingTechnicians (AAT)", "membershipNumber" -> "123445")

      val result = await(controller.submitAntiMoneyLaundering(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showCheckYourAnswers().url

      val session = await(sessionStoreService.fetchAgentSession).get

      session.amlsDetails shouldBe Some(AmlsDetails("Association of AccountingTechnicians (AAT)", Some("123445")))

      //should revert to normal state after amending is successful
      session.changingAnswers shouldBe false
    }

    "show validation error when form params are incorrect" in {
      await(sessionStoreService.cacheAgentSession(AgentSession(changingAnswers = true)))

      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("amlsBody" -> "", "membershipNumber" -> "123445")

      val result = await(controller.submitAntiMoneyLaundering(authenticatedRequest))

      status(result) shouldBe 200

      result should containMessages("error.moneyLaunderingCompliance.amlsbody.blank")
    }
  }
}
