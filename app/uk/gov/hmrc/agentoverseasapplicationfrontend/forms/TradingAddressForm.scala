package uk.gov.hmrc.agentoverseasapplicationfrontend.forms

import play.api.data.Form
import play.api.data.Forms._
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.TradingAddress
import uk.gov.hmrc.agentoverseasapplicationfrontend.forms.FormValidators.countryCode

object TradingAddressForm {

  def tradingAddressForm(validCountryCodes: Set[String]): Form[TradingAddress] =
    Form[TradingAddress](
      mapping(
        "addressLine1" -> nonEmptyText,
        "addressLine2" -> nonEmptyText,
        "addressLine3" -> optional(text),
        "addressLine4" -> optional(text),
        "countryCode"  -> countryCode(validCountryCodes)
      )(TradingAddress.apply)(TradingAddress.unapply))
}
