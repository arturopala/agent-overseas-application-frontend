package uk.gov.hmrc.agentoverseasapplicationfrontend.forms

import play.api.data.Form
import play.api.data.Forms._
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.MainBusinessAddress
import uk.gov.hmrc.agentoverseasapplicationfrontend.forms.FormValidators.countryCode

object MainBusinessAddressForm {

  def mainBusinessAddressForm(validCountryCodes: Set[String]): Form[MainBusinessAddress] =
    Form[MainBusinessAddress](
      mapping(
        "addressLine1" -> nonEmptyText,
        "addressLine2" -> nonEmptyText,
        "addressLine3" -> optional(text),
        "addressLine4" -> optional(text),
        "countryCode"  -> countryCode(validCountryCodes)
      )(MainBusinessAddress.apply)(MainBusinessAddress.unapply))
}
