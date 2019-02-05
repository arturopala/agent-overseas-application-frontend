package uk.gov.hmrc.agentoverseasapplicationfrontend.support

import com.google.inject.AbstractModule
import org.jsoup.Jsoup
import org.scalatest.matchers.{MatchResult, Matcher}
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.agentoverseasapplicationfrontend.services.SessionStoreService
import uk.gov.hmrc.agentoverseasapplicationfrontend.stubs.{AuthStubs, DataStreamStubs}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.test.UnitSpec

class BaseISpec extends UnitSpec with OneAppPerSuite with WireMockSupport with AuthStubs with DataStreamStubs with MetricsTestSupport with DefaultAwaitTimeout {
  implicit val ec = play.api.libs.concurrent.Execution.defaultContext

  override implicit lazy val app: Application = appBuilder.build()

  protected def appBuilder: GuiceApplicationBuilder = {
    new GuiceApplicationBuilder()
      .configure(
        "appName" -> "agent-overseas-application-frontend",
        "microservice.services.auth.port" -> wireMockPort,
        "microservice.services.companyAuthSignInUrl" -> "/baseISpec/gg/sign-in",
        "government-gateway-registration-frontend.sosRedirect-path" -> "/government-gateway-registration-frontend?accountType=agent&origin=unknown",
        "microservice.services.agent-overseas-application.host" -> wireMockHost,
        "microservice.services.agent-overseas-application.port" -> wireMockPort,
        "cachable.session-cache.port" -> wireMockPort,
        "cachable.session-cache.domain" -> "keystore",
        "maintainer-application-review-days" -> 28,
        "feedback-survey-url" -> "http://localhost:9514/feedback/OVERSEAS_AGENTS",
        "metrics.enabled" -> true,
        "auditing.enabled" -> true,
        "auditing.consumer.baseUri.host" -> wireMockHost,
        "auditing.consumer.baseUri.port" -> wireMockPort)
      .overrides(new TestGuiceModule)
  }

  override def commonStubs(): Unit = {
    givenCleanMetricRegistry()
    givenAuditConnector()
  }

  protected lazy val sessionStoreService = new TestSessionStoreService

  private class TestGuiceModule extends AbstractModule {
    override def configure(): Unit = {
      bind(classOf[SessionStoreService]).toInstance(sessionStoreService)
    }
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    sessionStoreService.clear()
  }

  protected implicit val materializer = app.materializer

  protected def checkHtmlResultWithBodyText(result: Result, expectedSubstring: String): Unit = {
    status(result) shouldBe 200
    contentType(result) shouldBe Some("text/html")
    charset(result) shouldBe Some("utf-8")
    bodyOf(result) should include(expectedSubstring)
  }

  private val messagesApi = app.injector.instanceOf[MessagesApi]
  private implicit val messages: Messages = messagesApi.preferred(Seq.empty[Lang])

  protected def htmlEscapedMessage(key: String): String = HtmlFormat.escape(Messages(key)).toString
  protected def htmlEscapedMessage(key: String, args: Any*): String = HtmlFormat.escape(Messages(key, args: _*)).toString
  protected def htmlMessage(key: String, args: Any*): String = Messages(key, args: _*).toString

  implicit def hc(implicit request: FakeRequest[_]): HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

  protected def checkMessageIsDefined(messageKey: String) =
    withClue(s"Message key ($messageKey) should be defined: ") {
      Messages.isDefinedAt(messageKey) shouldBe true
    }

  protected def checkIsHtml200(result: Result) = {
    status(result) shouldBe 200
    charset(result) shouldBe Some("utf-8")
    contentType(result) shouldBe Some("text/html")
  }

  protected def containSubstrings(expectedSubstrings: String*): Matcher[Result] =
    new Matcher[Result] {
      override def apply(result: Result): MatchResult = {
        checkIsHtml200(result)

        val resultBody = bodyOf(result)
        val (strsPresent, strsMissing) = expectedSubstrings.partition { expectedSubstring =>
          expectedSubstring.trim should not be ""
          resultBody.contains(expectedSubstring)
        }

        MatchResult(
          strsMissing.isEmpty,
          s"Expected substrings are missing in the response: ${strsMissing.mkString("\"", "\", \"", "\"")}",
          s"Expected substrings are present in the response : ${strsPresent.mkString("\"", "\", \"", "\"")}"
        )
      }
    }

  protected def containMessages(expectedMessageKeys: String*): Matcher[Result] =
    new Matcher[Result] {
      override def apply(result: Result): MatchResult = {
        expectedMessageKeys.foreach(checkMessageIsDefined)
        checkIsHtml200(result)
        val resultBody = bodyOf(result)
        val (msgsPresent, msgsMissing) = expectedMessageKeys.partition { messageKey =>
          resultBody.contains(htmlEscapedMessage(messageKey))
        }
        MatchResult(
          msgsMissing.isEmpty,
          s"Content is missing in the response for message keys: ${msgsMissing.mkString(", ")}",
          s"Content is present in the response for message keys: ${msgsPresent.mkString(", ")}"
        )
      }
    }

  protected def containElement(id: String,
                               tag: String,
                               attrs: Map[String, String]): Matcher[Result] = {
    new Matcher[Result] {
      override def apply(result: Result): MatchResult = {
        val doc = Jsoup.parse(bodyOf(result))
        val foundElement = doc.getElementById(id)
        val isAsExpected = Option(foundElement) match {
          case None => false
          case Some(elFound) => {
            val isExpectedTag = elFound.tagName() == tag
            val hasExpectedAttrs = attrs.forall{ case (expectedAttr, expectedValue) =>
              elFound.attr(expectedAttr) == expectedValue
            }
            isExpectedTag && hasExpectedAttrs
          }
        }

        MatchResult(
          isAsExpected,
          s"""Response does not contain a "$tag" element with id of "$id" with matching attributes $attrs""",
          s"""Response contains a "$tag" element with id of "$id" with matching attributes $attrs"""
        )
      }
    }
  }

  protected def containSubmitButton(expectedMessageKey: String, expectedElementId: String, expectedTagName: String = "button", expectedType: String = "submit"): Matcher[Result] = {
    new Matcher[Result] {
      override def apply(result: Result): MatchResult = {
        val doc = Jsoup.parse(bodyOf(result))
        checkMessageIsDefined(expectedMessageKey)
        val foundElement = doc.getElementById(expectedElementId)
        val isAsExpected = Option(foundElement) match {
          case None => false
          case Some(element) => {
            val isExpectedTag = element.tagName() == expectedTagName
            val isExpectedType = element.attr("type") == expectedType
            val hasExpectedMsg = element.text() == htmlEscapedMessage(expectedMessageKey)
            isExpectedTag && isExpectedType && hasExpectedMsg
          }
        }
        MatchResult(
          isAsExpected,
          s"""Response does not contain a submit button with id "$expectedElementId" and type "$expectedType" with content for message key "$expectedMessageKey" """,
          s"""Response contains a submit button with id "$expectedElementId" and type "$expectedType" with content for message key "$expectedMessageKey" """
        )
      }
    }
  }

  protected def containLink(expectedMessageKey: String, expectedHref: String): Matcher[Result] = {
    new Matcher[Result] {
      override def apply(result: Result): MatchResult = {
        val doc = Jsoup.parse(bodyOf(result))
        checkMessageIsDefined(expectedMessageKey)
        val foundElement = doc.select(s"a[href=$expectedHref]").first()
        val wasFoundWithCorrectMessage = Option(foundElement) match {
          case None => false
          case Some(element) => element.text() == htmlEscapedMessage(expectedMessageKey)
        }
        MatchResult(
          wasFoundWithCorrectMessage,
          s"""Response does not contain a link to "$expectedHref" with content for message key "$expectedMessageKey" """,
          s"""Response contains a link to "$expectedHref" with content for message key "$expectedMessageKey" """
        )
      }
    }
  }

  protected def repeatMessage(expectedMessageKey: String, times: Int): Matcher[Result] = new Matcher[Result] {
    override def apply(result: Result): MatchResult = {
      checkIsHtml200(result)
      MatchResult(
        Messages(expectedMessageKey).r.findAllMatchIn(bodyOf(result)).size == times,
        s"The message keys $expectedMessageKey does not appear $times times in the content",
        s"The message keys $expectedMessageKey appears $times times in the content"
      )
    }
  }

}
