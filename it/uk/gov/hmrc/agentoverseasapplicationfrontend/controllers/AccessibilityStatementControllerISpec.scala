package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers

import play.api.test.FakeRequest
import uk.gov.hmrc.agentoverseasapplicationfrontend.stubs.AgentOverseasApplicationStubs
import uk.gov.hmrc.agentoverseasapplicationfrontend.support.BaseISpec

class AccessibilityStatementControllerISpec extends BaseISpec with AgentOverseasApplicationStubs {

  private lazy val controller = app.injector.instanceOf[AccessibilityStatementController]

  "GET /accessibility-statement" should {
    "show the accessibility statement content" in {
      val result = await(controller.showAccessibilityStatement(basicRequest(FakeRequest())))

      status(result) shouldBe 200
      result should containMessages("accessibility.statement.h1")
     }
  }
}