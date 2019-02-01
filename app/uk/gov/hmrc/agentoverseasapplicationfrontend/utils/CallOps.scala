package uk.gov.hmrc.agentoverseasapplicationfrontend.utils

import java.net.URLEncoder
import play.api.mvc.Call

object CallOps {

  implicit class CallOps(call: Call) {
    def toURLWithParams(params: (String, Option[String])*): String = addParamsToUrl(call.url, params: _*)
  }

  def addParamsToUrl(url: String, params: (String, Option[String])*): String = {
    val query = params collect { case (k, Some(v)) => s"$k=${URLEncoder.encode(v, "UTF-8")}" } mkString "&"
    if (query.isEmpty) {
      url
    } else if (url.endsWith("?") || url.endsWith("&")) {
      url + query
    } else {
      val join = if (url.contains("?")) "&" else "?"
      url + join + query
    }
  }
}
