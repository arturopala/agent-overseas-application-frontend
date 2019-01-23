package uk.gov.hmrc.agentoverseasapplicationfrontend.forms

import play.api.data.Form
import play.api.data.Forms._
import uk.gov.hmrc.agentoverseasapplicationfrontend.validators.CommonValidators.tradingName

object TradingNameForm {
  val form: Form[String] = Form(
    single(
      "tradingName" -> tradingName
    )
  )
}
