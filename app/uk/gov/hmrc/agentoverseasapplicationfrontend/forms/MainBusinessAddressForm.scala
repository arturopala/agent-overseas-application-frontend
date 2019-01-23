package uk.gov.hmrc.agentoverseasapplicationfrontend.forms

import play.api.data.Form
import play.api.data.Forms._
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.MainBusinessAddress
import uk.gov.hmrc.agentoverseasapplicationfrontend.validators.CommonValidators._

object MainBusinessAddressForm {

  def mainBusinessAddressForm(validCountryCodes: Set[String]): Form[MainBusinessAddress] =
    Form[MainBusinessAddress](
      mapping(
        "addressLine1" -> addressLine12(lineNumber = 1),
        "addressLine2" -> addressLine12(lineNumber = 2),
        "addressLine3" -> addressLine34(lineNumber = 3),
        "addressLine4" -> addressLine34(lineNumber = 4),
        "countryCode"  -> countryCode(validCountryCodes)
      )(MainBusinessAddress.apply)(MainBusinessAddress.unapply))
}
