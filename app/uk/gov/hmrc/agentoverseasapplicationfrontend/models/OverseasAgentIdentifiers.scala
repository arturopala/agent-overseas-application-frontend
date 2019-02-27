package uk.gov.hmrc.agentoverseasapplicationfrontend.models

import java.util.UUID

import uk.gov.hmrc.domain.{SimpleObjectReads, SimpleObjectWrites}

sealed trait OverseasAgentIdentifier { val value: String }

case class SaAgentCode(value: String) extends OverseasAgentIdentifier

object SaAgentCode {
  implicit val reads = new SimpleObjectReads[SaAgentCode]("value", SaAgentCode.apply)
  implicit val writes = new SimpleObjectWrites[SaAgentCode](_.value)
}

case class CtAgentCode(value: String) extends OverseasAgentIdentifier

object CtAgentCode {
  implicit val reads = new SimpleObjectReads[CtAgentCode]("value", CtAgentCode.apply)
  implicit val writes = new SimpleObjectWrites[CtAgentCode](_.value)
}

case class ApplicationReference(value: String) extends OverseasAgentIdentifier

object ApplicationReference {

  implicit val reads = new SimpleObjectReads[ApplicationReference]("value", ApplicationReference.apply)
  implicit val writes = new SimpleObjectWrites[ApplicationReference](_.value)

  def create(uuid: UUID = UUID.randomUUID()): ApplicationReference = {
    val formatId = uuid.toString.replace("-", "").take(8)
    ApplicationReference(formatId)
  }
}

case class SafeId(value: String) extends OverseasAgentIdentifier

object SafeId {
  implicit val reads = new SimpleObjectReads[SafeId]("value", SafeId.apply)
  implicit val writes = new SimpleObjectWrites[SafeId](_.value)
}

case class Crn(value: String) extends OverseasAgentIdentifier

object Crn {
  implicit val reads = new SimpleObjectReads[Crn]("value", Crn.apply)
  implicit val writes = new SimpleObjectWrites[Crn](_.value)
}

case class Trn(value: String) extends OverseasAgentIdentifier

object Trn {
  implicit val ordering = Ordering.by(unapply)
  implicit val reads = new SimpleObjectReads[Trn]("value", Trn.apply)
  implicit val writes = new SimpleObjectWrites[Trn](_.value)
}
