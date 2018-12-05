package uk.gov.hmrc.agentoverseasapplicationfrontend.forms

import play.api.data.Form
import play.api.data.Forms._

import uk.gov.hmrc.agentoverseasapplicationfrontend.models.AmlsDetails

object AmlsDetailsForm {

  val form: Form[AmlsDetails] = Form[AmlsDetails](
    mapping(
      "amlsBody"         -> nonEmptyText,
      "membershipNumber" -> optional(text)
    )(AmlsDetails.apply)(AmlsDetails.unapply))
}
