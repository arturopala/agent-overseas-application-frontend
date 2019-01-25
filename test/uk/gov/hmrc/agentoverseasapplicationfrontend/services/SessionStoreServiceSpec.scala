package uk.gov.hmrc.agentoverseasapplicationfrontend.services

import uk.gov.hmrc.agentoverseasapplicationfrontend.models.PersonalDetails.RadioOption
import uk.gov.hmrc.agentoverseasapplicationfrontend.models._
import uk.gov.hmrc.agentoverseasapplicationfrontend.support.TestSessionCache
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.play.test.UnitSpec

import scala.collection.immutable.SortedSet
import scala.concurrent.ExecutionContext.Implicits.global

class SessionStoreServiceSpec extends UnitSpec {

  implicit val hc = HeaderCarrier(sessionId = Some(SessionId("sessionId123456")))

  private val contactDetails = ContactDetails("test", "last", "senior agent", "12345", "test@email.com")
  private val amlsDetails = AmlsDetails("Keogh Chartered Accountants", Some("123456"))
  private val mainBusinessAddress = MainBusinessAddress("line 1", "line 2", None, None, countryCode = "IE")
  private val personalDetails = PersonalDetails(Some(RadioOption.NinoChoice), Some(Nino("AB123456A")), None)
  private val agentCodes = AgentCodes(Some("SA123456"), Some("CT123456"), Some("VAT123456"), Some("PAYE123456"))

  private val crn = CompanyRegistrationNumber(Some(true), Some("123456"))

  private val agentSession = AgentSession(
    amlsDetails = Some(amlsDetails),
    contactDetails = Some(contactDetails),
    tradingName = Some("Trading name"),
    mainBusinessAddress = Some(mainBusinessAddress),
    registeredWithHmrc = Some(Yes),
    agentCodes = None,
    registeredForUkTax = Some(No),
    personalDetails = None,
    companyRegistrationNumber = Some(crn),
    hasTaxRegNumbers = Some(true),
    taxRegistrationNumbers = Some(SortedSet("123", "456")),
    changingAnswers = false
  )

  "SessionStoreService AgentSession" should {

    "store agent details" in {
      val store = new SessionStoreService(new TestSessionCache())

      await(store.cacheAgentSession(agentSession))

      await(store.fetchAgentSession) shouldBe Some(agentSession)
    }

    "always sanitise data when stored" in {
      val store = new SessionStoreService(new TestSessionCache())

      await(
        store.cacheAgentSession(agentSession
          .copy(registeredWithHmrc = Some(No), agentCodes = Some(agentCodes), personalDetails = Some(personalDetails))))

      await(store.fetchAgentSession).get.agentCodes shouldBe None
      await(store.fetchAgentSession).get.personalDetails shouldBe None
    }

    "return None when no application details have been stored" in {
      val store = new SessionStoreService(new TestSessionCache())

      await(store.fetchAgentSession) shouldBe None
    }
  }
}
