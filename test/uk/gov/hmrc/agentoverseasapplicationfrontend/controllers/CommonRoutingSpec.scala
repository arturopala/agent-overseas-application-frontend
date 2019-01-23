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
  private val personalDetails = PersonalDetails(Some(RadioOption.NinoChoice), Some(Nino("AB123456A")), None)
  private val companyRegistrationNumber = CompanyRegistrationNumber(Some(true), Some("123"))

  private val detailsUpToRegisteredWithHmrc =
    AgentSession(
      amlsDetails = Some(amlsDetails),
      contactDetails = Some(contactDetails),
      tradingName = Some("some name"),
      mainBusinessAddress = Some(mainBusinessAddress)
    )

  "lookupNextPage" should {
    "return showAntiMoneyLaunderingForm when AmlsDetails are not found in session" in {
      await(FakeRouting.sessionStoreService.cacheAgentSession(detailsUpToRegisteredWithHmrc.copy(amlsDetails = None)))

      await(FakeRouting.lookupNextPage) shouldBe routes.ApplicationController.showAntiMoneyLaunderingForm()
    }

    "return showAntiMoneyLaunderingForm when session not found" in {
      await(FakeRouting.lookupNextPage) shouldBe routes.ApplicationController.showAntiMoneyLaunderingForm()
    }

    "return showContactDetailsForm when ContactDetails are not found in session" in {
      await(
        FakeRouting.sessionStoreService.cacheAgentSession(detailsUpToRegisteredWithHmrc.copy(contactDetails = None)))

      await(FakeRouting.lookupNextPage) shouldBe routes.ApplicationController.showContactDetailsForm()
    }

    "return showTradingNameForm when Trading Name is not found in session" in {
      await(FakeRouting.sessionStoreService.cacheAgentSession(detailsUpToRegisteredWithHmrc.copy(tradingName = None)))

      await(FakeRouting.lookupNextPage) shouldBe routes.ApplicationController.showTradingNameForm()
    }

    "return showMainBusinessAddressForm when Business Address is not found in session" in {
      await(
        FakeRouting.sessionStoreService.cacheAgentSession(
          detailsUpToRegisteredWithHmrc.copy(mainBusinessAddress = None)))

      await(FakeRouting.lookupNextPage) shouldBe routes.ApplicationController.showMainBusinessAddressForm()
    }

    "return showRegisteredWithHmrc when RegisteredWithHmrc choice is not found in session" in {
      await(
        FakeRouting.sessionStoreService.cacheAgentSession(
          detailsUpToRegisteredWithHmrc.copy(registeredWithHmrc = None)))

      await(FakeRouting.lookupNextPage) shouldBe routes.ApplicationController.showRegisteredWithHmrcForm()
    }

    "return correct branching page after having decided if they are registered with HMRC" when {
      "RegisteredWithHmrc choice is Yes" should {
        "return showSelfAssessmentAgentCodeForm when self assessment details are not in session" in {
          await(
            FakeRouting.sessionStoreService.cacheAgentSession(
              detailsUpToRegisteredWithHmrc.copy(registeredWithHmrc = Some(Yes))))

          await(FakeRouting.lookupNextPage) shouldBe routes.ApplicationController.showAgentCodesForm()
        }
      }

      s"RegisteredWithHmrc choice is No" should {
        "return showUkTaxRegistrationForm when uk tax registration details are not in session" in {
          await(
            FakeRouting.sessionStoreService.cacheAgentSession(
              detailsUpToRegisteredWithHmrc.copy(
                registeredWithHmrc = Some(No)
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
            detailsUpToRegisteredWithHmrc.copy(
              registeredWithHmrc = Some(No),
              registeredForUkTax = Some(Yes),
              personalDetails = None
            )))

        await(FakeRouting.lookupNextPage) shouldBe routes.ApplicationController.showPersonalDetailsForm()
      }

      "return showCompanyRegistrationNumberForm when personal details are in session" in {
        await(
          FakeRouting.sessionStoreService.cacheAgentSession(
            detailsUpToRegisteredWithHmrc.copy(
              registeredWithHmrc = Some(No),
              registeredForUkTax = Some(Yes),
              personalDetails = Some(personalDetails)
            )))

        await(FakeRouting.lookupNextPage) shouldBe routes.ApplicationController.showCompanyRegistrationNumberForm()
      }
    }

    s"RegisteredForUkTax choice is No" should {
      "return showCompanyRegistrationNumberForm when company registration number is not in session" in {
        await(
          FakeRouting.sessionStoreService.cacheAgentSession(
            detailsUpToRegisteredWithHmrc.copy(
              registeredWithHmrc = Some(No),
              registeredForUkTax = Some(No),
              personalDetails = None
            )))

        await(FakeRouting.lookupNextPage) shouldBe routes.ApplicationController
          .showCompanyRegistrationNumberForm()
      }
    }
  }

  "return showTaxRegistrationNumberForm when AgentSession collected prerequisites" when {
    "RegisteredForUkTax choice is Yes and personal details have been submitted" in {
      await(
        FakeRouting.sessionStoreService.cacheAgentSession(detailsUpToRegisteredWithHmrc.copy(
          registeredWithHmrc = Some(No),
          registeredForUkTax = Some(Yes),
          personalDetails = Some(personalDetails),
          companyRegistrationNumber = Some(companyRegistrationNumber)
        )))

      await(FakeRouting.lookupNextPage) shouldBe routes.ApplicationController.showTaxRegistrationNumberForm()
    }
  }

  s"RegisteredForUkTax choice is no and personal details were not submitted" in {
    await(
      FakeRouting.sessionStoreService.cacheAgentSession(
        detailsUpToRegisteredWithHmrc.copy(
          registeredWithHmrc = Some(No),
          registeredForUkTax = Some(No),
          personalDetails = None,
          companyRegistrationNumber = Some(companyRegistrationNumber)
        )))

    await(FakeRouting.lookupNextPage) shouldBe routes.ApplicationController.showTaxRegistrationNumberForm()
  }

  "return showYourTaxRegNo when hasTaxRegNumbers equals Some(true)" in {
    await(
      FakeRouting.sessionStoreService.cacheAgentSession(detailsUpToRegisteredWithHmrc.copy(
        registeredWithHmrc = Some(No),
        registeredForUkTax = Some(No),
        personalDetails = None,
        companyRegistrationNumber = Some(companyRegistrationNumber),
        hasTaxRegNumbers = Some(true)
      )))

    await(FakeRouting.lookupNextPage) shouldBe routes.ApplicationController.showYourTaxRegNumbersForm()
  }

  "return showCheckYourAnswers when hasTaxRegNumbers equals Some(false)" in {
    await(
      FakeRouting.sessionStoreService.cacheAgentSession(detailsUpToRegisteredWithHmrc.copy(
        registeredWithHmrc = Some(No),
        registeredForUkTax = Some(No),
        personalDetails = None,
        companyRegistrationNumber = Some(companyRegistrationNumber),
        hasTaxRegNumbers = Some(false)
      )))

    await(FakeRouting.lookupNextPage) shouldBe routes.ApplicationController.showCheckYourAnswers()
  }

  "return showAgentCodesForm when registered with HMRC and no answer for agent codes has yet been given" in {
    await(
      FakeRouting.sessionStoreService.cacheAgentSession(
        detailsUpToRegisteredWithHmrc.copy(
          registeredWithHmrc = Some(Yes),
          agentCodes = None
        )))

    await(FakeRouting.lookupNextPage) shouldBe routes.ApplicationController.showAgentCodesForm()
  }

  "return showCheckYourAnswers when one or more agent codes have been given" in {
    await(
      FakeRouting.sessionStoreService.cacheAgentSession(
        detailsUpToRegisteredWithHmrc.copy(
          registeredWithHmrc = Some(Yes),
          agentCodes = Some(AgentCodes(Some("saCode"), None, None, None))
        )))

    await(FakeRouting.lookupNextPage) shouldBe routes.ApplicationController.showCheckYourAnswers()
  }

  "return correct branching page after having submitted no agent codes" when {
    "return showUkTaxRegistrationForm when they have not yet made a choice for whether they are registered for UK tax" in {
      await(
        FakeRouting.sessionStoreService.cacheAgentSession(
          detailsUpToRegisteredWithHmrc.copy(
            registeredWithHmrc = Some(Yes),
            agentCodes = Some(AgentCodes(None, None, None, None)),
            registeredForUkTax = None
          )))

      await(FakeRouting.lookupNextPage) shouldBe routes.ApplicationController.showUkTaxRegistrationForm()
    }

    "return showPersonalDetailsForm when an answer for agent codes was given, but no agent codes were supplied, and they've answered yes to UK Tax registration" in {
      await(
        FakeRouting.sessionStoreService.cacheAgentSession(
          detailsUpToRegisteredWithHmrc.copy(
            registeredWithHmrc = Some(Yes),
            agentCodes = Some(AgentCodes(None, None, None, None)),
            registeredForUkTax = Some(Yes),
            personalDetails = None
          )))

      await(FakeRouting.lookupNextPage) shouldBe routes.ApplicationController.showPersonalDetailsForm()
    }

    s"return showCompanyRegistrationNumberForm when Uk Tax registered choice was No" in {
      await(
        FakeRouting.sessionStoreService.cacheAgentSession(detailsUpToRegisteredWithHmrc.copy(
          registeredWithHmrc = Some(Yes),
          agentCodes = Some(AgentCodes(None, None, None, None)),
          registeredForUkTax = Some(No),
          personalDetails = None,
          companyRegistrationNumber = None
        )))

      await(FakeRouting.lookupNextPage) shouldBe routes.ApplicationController.showCompanyRegistrationNumberForm()
    }
  }

}

object FakeRouting extends CommonRouting with Results {
  override val sessionStoreService = new SessionStoreService(new TestSessionCache())
}
