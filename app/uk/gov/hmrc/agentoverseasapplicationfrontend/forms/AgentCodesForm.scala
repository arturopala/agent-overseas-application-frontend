package uk.gov.hmrc.agentoverseasapplicationfrontend.forms

import play.api.data.Form
import play.api.data.Forms._
import uk.gov.hmrc.agentoverseasapplicationfrontend.models.AgentCodes
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfTrue

object AgentCodesForm {

  def form: Form[AgentCodes] =
    Form[AgentCodes](
      mapping(
        "self-assessment-checkbox" -> boolean,
        "self-assessment"          -> mandatoryIfTrue("self-assessment-checkbox", nonEmptyText),
        "corporation-tax-checkbox" -> boolean,
        "corporation-tax"          -> mandatoryIfTrue("corporation-tax-checkbox", nonEmptyText),
        "vat-checkbox"             -> boolean,
        "vat"                      -> mandatoryIfTrue("vat-checkbox", nonEmptyText),
        "paye-checkbox"            -> boolean,
        "paye"                     -> mandatoryIfTrue("paye-checkbox", nonEmptyText)
      )(
        (hasSa, sa, hasCt, ct, hasVat, vat, hasPaye, paye) =>
          AgentCodes(
            sa.filter(_ => hasSa),
            ct.filter(_ => hasCt),
            vat.filter(_ => hasVat),
            paye.filter(_ => hasPaye)
        ))(
        (codes: AgentCodes) =>
          Some(
            (
              codes.selfAssessment.isDefined,
              codes.selfAssessment,
              codes.corporationTax.isDefined,
              codes.corporationTax,
              codes.vat.isDefined,
              codes.vat,
              codes.paye.isDefined,
              codes.paye
            )))
    )
}
