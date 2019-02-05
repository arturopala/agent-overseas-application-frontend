import java.net.URL

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import javax.inject.{Inject, Named, Provider, Singleton}
import org.slf4j.MDC
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.http.{HttpPost, _}
import uk.gov.hmrc.play.bootstrap.http.{DefaultHttpClient, HttpClient}
import uk.gov.hmrc.play.config.ServicesConfig

class FrontendModule(val environment: Environment, val configuration: Configuration)
    extends AbstractModule with ServicesConfig {

  override val runModeConfiguration: Configuration = configuration
  override protected def mode = environment.mode

  def configure(): Unit = {

    val appName = "agent-overseas-application-frontend"

    val loggerDateFormat: Option[String] = configuration.getString("logger.json.dateformat")
    Logger(getClass).info(s"Starting microservice : $appName : in mode : ${environment.mode}")
    MDC.put("appName", appName)
    loggerDateFormat.foreach(str => MDC.put("logger.json.dateformat", str))

    bind(classOf[HttpClient]).to(classOf[DefaultHttpClient])
    bind(classOf[HttpPost]).to(classOf[DefaultHttpClient])
    bind(classOf[HttpGet]).to(classOf[DefaultHttpClient])

    bindProperty("appName")
    bindProperty("country.list.location")
    bindProperty("feedback-survey-url")
    bindIntProperty("maintainer-application-review-days")

    bind(classOf[SessionCache]).to(classOf[ApplicationSessionCache])

    bindServiceConfigProperty[String]("cachable.session-cache.domain")
    bindServiceConfigProperty[String]("companyAuthSignInUrl")
    bindServiceConfigProperty[String]("agent-overseas-subscription-frontend.root-path")
    bindServiceConfigProperty[String]("government-gateway-registration-frontend.sosRedirect-path")
    bindServiceConfigProperty[String]("agent-services-account.root-path")

    bindBaseUrl("auth")
    bindBaseUrl("cachable.session-cache")
    bindBaseUrl("agent-overseas-application")
  }

  private def bindBaseUrl(serviceName: String) =
    bind(classOf[URL]).annotatedWith(Names.named(s"$serviceName-baseUrl")).toProvider(new BaseUrlProvider(serviceName))

  private class BaseUrlProvider(serviceName: String) extends Provider[URL] {
    override lazy val get = new URL(baseUrl(serviceName))
  }

  private def bindProperty(propertyName: String) =
    bind(classOf[String]).annotatedWith(Names.named(propertyName)).toProvider(new PropertyProvider(propertyName))

  private def bindIntProperty(propertyName: String) =
    bind(classOf[Int]).annotatedWith(Names.named(propertyName)).toProvider(new IntPropertyProvider(propertyName))

  private class PropertyProvider(confKey: String) extends Provider[String] {
    override lazy val get = configuration
      .getString(confKey)
      .getOrElse(throw new IllegalStateException(s"No value found for configuration property $confKey"))
  }

  private class IntPropertyProvider(confKey: String) extends Provider[Int] {
    override lazy val get = configuration
      .getInt(confKey)
      .getOrElse(throw new IllegalStateException(s"No value found for configuration property $confKey"))
  }

  import com.google.inject.binder.ScopedBindingBuilder
  import com.google.inject.name.Names.named

  import scala.reflect.ClassTag

  private def bindServiceConfigProperty[A](
    propertyName: String)(implicit classTag: ClassTag[A], ct: ServiceConfigPropertyType[A]): ScopedBindingBuilder =
    ct.bindServiceConfigProperty(classTag.runtimeClass.asInstanceOf[Class[A]])(propertyName)

  sealed trait ServiceConfigPropertyType[A] {
    def bindServiceConfigProperty(clazz: Class[A])(propertyName: String): ScopedBindingBuilder
  }

  object ServiceConfigPropertyType {

    implicit val stringServiceConfigProperty: ServiceConfigPropertyType[String] =
      new ServiceConfigPropertyType[String] {
        def bindServiceConfigProperty(clazz: Class[String])(propertyName: String): ScopedBindingBuilder =
          bind(clazz)
            .annotatedWith(named(s"$propertyName"))
            .toProvider(new StringServiceConfigPropertyProvider(propertyName))

        private class StringServiceConfigPropertyProvider(propertyName: String) extends Provider[String] {
          override lazy val get = getConfString(
            propertyName,
            throw new RuntimeException(s"No service configuration value found for '$propertyName'"))
        }
      }

    implicit val intServiceConfigProperty: ServiceConfigPropertyType[Int] = new ServiceConfigPropertyType[Int] {
      def bindServiceConfigProperty(clazz: Class[Int])(propertyName: String): ScopedBindingBuilder =
        bind(clazz)
          .annotatedWith(named(s"$propertyName"))
          .toProvider(new IntServiceConfigPropertyProvider(propertyName))

      private class IntServiceConfigPropertyProvider(propertyName: String) extends Provider[Int] {
        override lazy val get = getConfInt(
          propertyName,
          throw new RuntimeException(s"No service configuration value found for '$propertyName'"))
      }
    }

    implicit val booleanServiceConfigProperty: ServiceConfigPropertyType[Boolean] =
      new ServiceConfigPropertyType[Boolean] {
        def bindServiceConfigProperty(clazz: Class[Boolean])(propertyName: String): ScopedBindingBuilder =
          bind(clazz)
            .annotatedWith(named(s"$propertyName"))
            .toProvider(new BooleanServiceConfigPropertyProvider(propertyName))

        private class BooleanServiceConfigPropertyProvider(propertyName: String) extends Provider[Boolean] {
          override lazy val get = getConfBool(propertyName, false)
        }
      }
  }
}

@Singleton
class ApplicationSessionCache @Inject()(
  val http: HttpClient,
  @Named("appName") val appName: String,
  @Named("cachable.session-cache.domain") val domain: String,
  @Named("cachable.session-cache-baseUrl") val baseUrl: URL)
    extends SessionCache {
  override lazy val defaultSource = appName
  override lazy val baseUri = baseUrl.toExternalForm
}
