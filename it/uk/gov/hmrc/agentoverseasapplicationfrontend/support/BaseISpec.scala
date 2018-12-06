package uk.gov.hmrc.agentoverseasapplicationfrontend.support

import com.google.inject.AbstractModule
import org.scalatest.matchers.{MatchResult, Matcher}
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.agentoverseasapplicationfrontend.services.SessionStoreService
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.agentoverseasapplicationfrontend.stubs.{AuthStubs, DataStreamStubs}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

class BaseISpec extends UnitSpec with OneAppPerSuite with WireMockSupport with AuthStubs with DataStreamStubs with MetricsTestSupport {
  implicit val ec = play.api.libs.concurrent.Execution.defaultContext

  override implicit lazy val app: Application = appBuilder.build()

  protected def appBuilder: GuiceApplicationBuilder = {
    new GuiceApplicationBuilder()
      .configure(
        "appName" -> "agent-overseas-application-frontend",
        "microservice.services.auth.port" -> wireMockPort,
        "microservice.services.companyAuthSignInUrl" -> "/baseISpec/gg/sign-in",
        "cachable.session-cache.port" -> wireMockPort,
        "cachable.session-cache.domain" -> "keystore",
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
