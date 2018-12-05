package uk.gov.hmrc.agentoverseasapplicationfrontend.models

import play.api.libs.json.Json
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.domain.Nino

case class AgentApplication(amlsDetails: AmlsDetails)

object AgentApplication {
  implicit val format = Json.format[AgentApplication]
}
