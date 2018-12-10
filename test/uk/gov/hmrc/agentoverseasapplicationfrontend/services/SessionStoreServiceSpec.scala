package uk.gov.hmrc.agentoverseasapplicationfrontend.services

import uk.gov.hmrc.agentoverseasapplicationfrontend.models._
import uk.gov.hmrc.agentoverseasapplicationfrontend.support.TestSessionCache
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global

class SessionStoreServiceSpec extends UnitSpec {

  implicit val hc = HeaderCarrier(sessionId = Some(SessionId("sessionId123456")))

  private val contactDetails = ContactDetails("test", "last", "senior agent", "12345", "test@email.com")
  private val amlsDetails = AmlsDetails("Keogh Chartered Accountants", Some("123456"))
  private val registeredWithHmrc = Yes

  private val agentSession =
    AgentSession(
      amlsDetails = Some(amlsDetails),
      contactDetails = Some(contactDetails),
      registeredWithHmrc = Some(registeredWithHmrc)
    )

  "SessionStoreService" should {

    "store agent details" in {
      val store = new SessionStoreService(new TestSessionCache())

      await(store.cacheAgentSession(agentSession))

      await(store.fetchAgentSession) shouldBe Some(agentSession)
    }

    "return None when no application details have been stored" in {
      val store = new SessionStoreService(new TestSessionCache())

      await(store.fetchAgentSession) shouldBe None
    }

    "remove the underlying storage for the current session when remove is called" in {
      val store = new SessionStoreService(new TestSessionCache())

      await(store.cacheAgentSession(agentSession))

      await(store.remove())

      await(store.fetchAgentSession) shouldBe None
    }
  }
}
