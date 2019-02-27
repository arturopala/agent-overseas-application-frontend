package uk.gov.hmrc.agentoverseasapplicationfrontend.forms

import play.api.data.Form
import play.api.data.Forms._
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.{AgentCodes, CtAgentCode, SaAgentCode}
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
            sa.collect { case x if hasSa => SaAgentCode(x) },
            ct.collect { case x if hasCt => CtAgentCode(x) }
        ))(
        (codes: AgentCodes) =>
          Some(
            (
              codes.selfAssessment.isDefined,
              codes.selfAssessment.map(_.value),
              codes.corporationTax.isDefined,
              codes.corporationTax.map(_.value)
            )))
    )
}
