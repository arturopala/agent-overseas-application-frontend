package uk.gov.hmrc.agentoverseasapplicationfrontend.controllers

import play.api.mvc.Results
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.PersonalDetails.RadioOption
import uk.gov.hmrc.agentoverseasapplicationfrontend.models._
import uk.gov.hmrc.agentoverseasapplicationfrontend.services.SessionStoreService
import uk.gov.hmrc.agentoverseasapplicationfrontend.support.TestSessionCache
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global

class CommonRoutingSpec extends UnitSpec {
  implicit val hc = HeaderCarrier(sessionId = Some(SessionId("sessionId123456")))

  private val contactDetails = ContactDetails("test", "last", "senior agent", "12345", "test@email.com")
  private val amlsDetails = AmlsDetails("Keogh Chartered Accountants", Some("123456"))
  private val mainBusinessAddress = MainBusinessAddress("line1", "line2", None, None, "IE")
  private val personalDetails = PersonalDetails(RadioOption.NinoChoice, Some(Nino("AB123456A")), None)

  private val agentSession =
    AgentSession(
      amlsDetails = Some(amlsDetails),
      contactDetails = Some(contactDetails),
      tradingName = Some("some name"),
      mainBusinessAddress = Some(mainBusinessAddress),
      personalDetails = Some(personalDetails)
    )

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

    "return showMainBusinessAddressForm when Business Address is not found in session" in {
      await(FakeRouting.sessionStoreService.cacheAgentSession(agentSession.copy(mainBusinessAddress = None)))

      await(FakeRouting.lookupNextPage) shouldBe routes.ApplicationController.showMainBusinessAddressForm()
    }

    "return showRegisteredWithHmrc when RegisteredWithHmrc choice is not found in session" in {
      await(FakeRouting.sessionStoreService.cacheAgentSession(agentSession.copy(registeredWithHmrc = None)))

      await(FakeRouting.lookupNextPage) shouldBe routes.ApplicationController.showRegisteredWithHmrcForm()
    }

    "return correct branching page after having decided if they are registered with HMRC" when {
      "RegisteredWithHmrc choice is Yes" should {
        "return showSelfAssessmentAgentCodeForm when self assessment details are not in session" in {
          await(FakeRouting.sessionStoreService.cacheAgentSession(agentSession.copy(registeredWithHmrc = Some(Yes))))

          await(FakeRouting.lookupNextPage) shouldBe routes.ApplicationController.showSelfAssessmentAgentCodeForm()
        }
      }

      Seq(No, Unsure).foreach { registeredWithHmrcChoice =>
        s"RegisteredWithHmrc choice is $registeredWithHmrcChoice" should {
          "return showUkTaxRegistrationForm when uk tax registration details are not in session" in {
            await(
              FakeRouting.sessionStoreService.cacheAgentSession(agentSession.copy(
                registeredWithHmrc = Some(registeredWithHmrcChoice)
              )))

            await(FakeRouting.lookupNextPage) shouldBe routes.ApplicationController.showUkTaxRegistrationForm()
          }
        }
      }
    }

    "return correct branching page after having decided if they are registered for UK tax" when {
      "RegisteredForUkTax choice is Yes" should {
        "return showPersonalDetailsForm when personal details are not in session" in {
          await(
            FakeRouting.sessionStoreService.cacheAgentSession(
              agentSession.copy(
                registeredWithHmrc = Some(Unsure),
                registeredForUkTax = Some(Yes),
                personalDetails = None
              )))

          await(FakeRouting.lookupNextPage) shouldBe routes.ApplicationController.showPersonalDetailsForm()
        }
      }

      Seq(No, Unsure).foreach { negativeChoice =>
        s"RegisteredForUkTax choice is $negativeChoice" should {
          "return showCompanyRegistrationNumberForm when company registration number is not in session" in {
            await(
              FakeRouting.sessionStoreService.cacheAgentSession(
                agentSession.copy(
                  registeredWithHmrc = Some(Unsure),
                  registeredForUkTax = Some(negativeChoice)
                )))

            await(FakeRouting.lookupNextPage) shouldBe routes.ApplicationController
              .showCompanyRegistrationNumberForm()
          }
        }
      }
    }

    "return showPersonalDetailsForm when PersonalDetails are not found in the session" in {
      await(
        FakeRouting.sessionStoreService.cacheAgentSession(
          agentSession.copy(registeredWithHmrc = Some(No), registeredForUkTax = Some(Yes), personalDetails = None)))

      await(FakeRouting.lookupNextPage) shouldBe routes.ApplicationController.showPersonalDetailsForm()
    }

    "return showTaxRegistrationNumberForm when AgentSession collected prerequisites" in {
      await(
        FakeRouting.sessionStoreService.cacheAgentSession(
          agentSession.copy(
            registeredWithHmrc = Some(No),
            registeredForUkTax = Some(Yes),
            companyRegistrationNumber = Some("someCompanyRegNo")
          )))

      await(FakeRouting.lookupNextPage) shouldBe routes.ApplicationController.showTaxRegistrationNumberForm()
    }

    Seq(No, Unsure).foreach { negativeChoice =>
      s"RegisteredForUkTax choice is $negativeChoice" should {
        "return showTaxRegistrationNumberForm when AgentSession collected prerequisites" in {
          await(
            FakeRouting.sessionStoreService.cacheAgentSession(
              agentSession.copy(
                registeredWithHmrc = Some(No),
                registeredForUkTax = Some(negativeChoice),
                personalDetails = None,
                companyRegistrationNumber = Some("someCompanyRegNo")
              )))

          await(FakeRouting.lookupNextPage) shouldBe routes.ApplicationController.showTaxRegistrationNumberForm()
        }
      }
    }

    "return showTaxRegistrationNumberForm when hasTaxRegNumbers equals None" in {
      await(
        FakeRouting.sessionStoreService.cacheAgentSession(
          agentSession.copy(
            registeredWithHmrc = Some(No),
            registeredForUkTax = Some(No),
            personalDetails = None,
            companyRegistrationNumber = Some("someCompanyRegNo"),
            hasTaxRegNumbers = None
          )))

      await(FakeRouting.lookupNextPage) shouldBe routes.ApplicationController.showTaxRegistrationNumberForm()
    }

    "return showYourTaxRegNo when hasTaxRegNumbers equals Some(true)" in {
      await(
        FakeRouting.sessionStoreService.cacheAgentSession(agentSession.copy(
          registeredWithHmrc = Some(No),
          registeredForUkTax = Some(No),
          personalDetails = None,
          companyRegistrationNumber = Some("someCompanyRegNo"),
          hasTaxRegNumbers = Some(true)
        )))

      await(FakeRouting.lookupNextPage) shouldBe routes.ApplicationController.showYourTaxRegNo()
    }

    "return showCheckAnswers when hasTaxRegNumbers equals Some(false)" in {
      await(
        FakeRouting.sessionStoreService.cacheAgentSession(agentSession.copy(
          registeredWithHmrc = Some(No),
          registeredForUkTax = Some(No),
          personalDetails = None,
          companyRegistrationNumber = Some("someCompanyRegNo"),
          hasTaxRegNumbers = Some(false)
        )))

      await(FakeRouting.lookupNextPage) shouldBe routes.ApplicationController.showCheckAnswers()
    }
  }

  object FakeRouting extends CommonRouting with Results {
    override val sessionStoreService = new SessionStoreService(new TestSessionCache())
  }

}
