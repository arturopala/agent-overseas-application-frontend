package uk.gov.hmrc.agentoverseasapplicationfrontend.forms

import play.api.data.Form
import play.api.data.Forms._
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.AgentCodes
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfTrue
import uk.gov.hmrc.agentoverseasapplicationfrontend.validators.CommonValidators._

object AgentCodesForm {

  def form: Form[AgentCodes] =
    Form[AgentCodes](
      mapping(
        "self-assessment-checkbox" -> boolean,
        "self-assessment"          -> mandatoryIfTrue("self-assessment-checkbox", saAgentCode),
        "corporation-tax-checkbox" -> boolean,
        "corporation-tax"          -> mandatoryIfTrue("corporation-tax-checkbox", ctAgentCode)
      )(
        (hasSa, sa, hasCt, ct) =>
          AgentCodes(
            sa.filter(_ => hasSa),
            ct.filter(_ => hasCt)
        ))(
        (codes: AgentCodes) =>
          Some(
            (
              codes.selfAssessment.isDefined,
              codes.selfAssessment,
              codes.corporationTax.isDefined,
              codes.corporationTax
            )))
    )
}
