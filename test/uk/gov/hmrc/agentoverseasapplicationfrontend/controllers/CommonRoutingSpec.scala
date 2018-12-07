package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers

import play.api.mvc.Results
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.{AgentSession, AmlsDetails, ContactDetails}
import uk.gov.hmrc.agentoverseasapplicationfrontend.services.SessionStoreService
import uk.gov.hmrc.agentoverseasapplicationfrontend.support.TestSessionCache
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global

class CommonRoutingSpec extends UnitSpec {
  implicit val hc = HeaderCarrier(sessionId = Some(SessionId("sessionId123456")))

  private val contactDetails = ContactDetails("test", "last", "senior agent", "12345", "test@email.com")
  private val amlsDetails = AmlsDetails("Keogh Chartered Accountants", Some("123456"))

  private val agentSession =
    AgentSession(Some(amlsDetails), contactDetails = Some(contactDetails), tradingName = Some("some name"))

  "lookupNextPage" should {
    "return showAntiMoneyLaunderingForm when AmlsDetails are not found in session" in {
      await(FakeRouting.sessionStoreService.cacheAgentSession(agentSession.copy(amlsDetails = None)))

      await(FakeRouting.lookupNextPage) shouldBe routes.ApplicationController.showAntiMoneyLaunderingForm()
    }

    "return showAntiMoneyLaunderingForm when session not found" in {
      await(FakeRouting.sessionStoreService.cacheAgentSession(agentSession.copy(amlsDetails = None)))

      await(FakeRouting.lookupNextPage) shouldBe routes.ApplicationController.showAntiMoneyLaunderingForm()
    }

    "return showContactDetailsForm when ContactDetails are not found in session" in {
      await(FakeRouting.sessionStoreService.cacheAgentSession(agentSession.copy(contactDetails = None)))

      await(FakeRouting.lookupNextPage) shouldBe routes.ApplicationController.showContactDetailsForm()
    }

    "return showTradingNameForm when Trading Name is not found in session" in {
      await(FakeRouting.sessionStoreService.cacheAgentSession(agentSession.copy(tradingName = None)))

      await(FakeRouting.lookupNextPage) shouldBe routes.ApplicationController.showTradingNameForm()
    }
  }

  object FakeRouting extends CommonRouting with Results {
    override val sessionStoreService = new SessionStoreService(new TestSessionCache())
  }

}
