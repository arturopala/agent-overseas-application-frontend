package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers

import play.api.test.Helpers.LOCATION
import play.api.test.FakeRequest
import uk.gov.hmrc.agentoverseasapplicationfrontend.support.BaseISpec

class ApplicationControllerISpec extends BaseISpec {

  private lazy val controller: ApplicationController = app.injector.instanceOf[ApplicationController]

  "GET /money-laundering" should {
    "display the money-laundering form" in {
      val authenticatedRequest = cleanCredsAgent(FakeRequest())

      val result = await(controller.showAntiMoneyLaunderingForm(authenticatedRequest))

      status(result) shouldBe 200

      result should containMessages(
        "amls.title",
        "amls.inset.p1",
        "amls.form.supervisory_body",
        "amls.form.membership_number",
        "amls.hint.expandable",
        "amls.hint.expandable.p1"
      )

      result should containSubstrings("https://www.gov.uk/guidance/get-an-hmrc-agent-services-account",
        routes.SignOutController.signOut().url)
    }
  }

  "POST /money-laundering" should {
    "redirect to contact-details" in {
      implicit val authenticatedRequest = cleanCredsAgent(FakeRequest())
        .withFormUrlEncodedBody("amlsBody" -> "ABCD", "membershipNumber" -> "123445")

      val result = await(controller.submitAntiMoneyLaundering(authenticatedRequest))

      status(result) shouldBe 303
      result.header.headers(LOCATION) shouldBe routes.ApplicationController.showContactDetailsForm().url

      val amlsDetails = await(sessionStoreService.fetchAgentApplication).get.amlsDetails

      amlsDetails.supervisoryBody shouldBe "ABCD"
      amlsDetails.membershipNumber shouldBe Some("123445")
    }
  }
}
