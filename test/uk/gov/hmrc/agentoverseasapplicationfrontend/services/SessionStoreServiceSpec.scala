package uk.gov.hmrc.agentoverseasapplicationfrontend.services

import uk.gov.hmrc.agentoverseasapplicationfrontend.models.{AgentApplication, AmlsDetails}
import uk.gov.hmrc.agentoverseasapplicationfrontend.support.TestSessionCache
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global

class SessionStoreServiceSpec extends UnitSpec {

  implicit val hc = HeaderCarrier(sessionId = Some(SessionId("sessionId123456")))

  private val agentApplication =
    AgentApplication(AmlsDetails("Keogh Chartered Accountants", Some("123456")))

  "SessionStoreService" should {

    "store application details" in {
      val store = new SessionStoreService(new TestSessionCache())

      await(store.cacheAgentApplication(agentApplication))

      await(store.fetchAgentApplication) shouldBe Some(agentApplication)
    }

    "return None when no application details have been stored" in {
      val store = new SessionStoreService(new TestSessionCache())

      await(store.fetchAgentApplication) shouldBe None
    }

    "remove the underlying storage for the current session when remove is called" in {
      val store = new SessionStoreService(new TestSessionCache())

      await(store.cacheAgentApplication(agentApplication))

      await(store.remove())

      await(store.fetchAgentApplication) shouldBe None
    }
  }
}
