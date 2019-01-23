package uk.gov.hmrc.agentoverseasapplicationfrontend.forms

import play.api.data.Form
import play.api.data.Forms._
import uk.gov.hmrc.agentoverseasapplicationfrontend.validators.CommonValidators._
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.AmlsDetails

object AmlsDetailsForm {

  def form(codes: Set[String]): Form[AmlsDetails] =
    Form[AmlsDetails](
      mapping(
        "amlsBody"         -> amlsCode(codes),
        "membershipNumber" -> membershipNumber
      )(AmlsDetails.apply)(AmlsDetails.unapply))
}
