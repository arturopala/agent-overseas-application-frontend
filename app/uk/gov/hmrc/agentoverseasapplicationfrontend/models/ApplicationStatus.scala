package uk.gov.hmrc.agentoverseasapplicationfrontend.models

import play.api.libs.json.{Format, Json}

sealed trait ApplicationStatus extends Product with Serializable {

  val key: String =
    this match { //status can only progress in acceding order indicated below
      case ApplicationStatus.Pending  => "pending" //1
      case ApplicationStatus.Rejected => "rejected" //2
      case ApplicationStatus.Accepted => "accepted" //2
      case ApplicationStatus.AttemptingRegistration =>
        "attempting_registration" //3
      case ApplicationStatus.Registered => "registered" //4
      case ApplicationStatus.Complete   => "complete" //5
    }

  val transitionFromStates: Seq[ApplicationStatus] = Seq.empty
}

object ApplicationStatus {

  case object Pending extends ApplicationStatus

  case object Rejected extends ApplicationStatus {
    override val transitionFromStates = Seq(Pending)
  }

  case object Accepted extends ApplicationStatus {
    override val transitionFromStates = Seq(Pending)
  }

  case object AttemptingRegistration extends ApplicationStatus {
    override val transitionFromStates = Seq(Accepted)
  }

  case object Registered extends ApplicationStatus {
    override val transitionFromStates = Seq(AttemptingRegistration)
  }

  case object Complete extends ApplicationStatus {
    override val transitionFromStates = Seq(Registered)
  }

  def apply(typeIdentifier: String): ApplicationStatus = typeIdentifier match {

    case ApplicationStatus.Pending.key  => ApplicationStatus.Pending
    case ApplicationStatus.Rejected.key => ApplicationStatus.Rejected
    case ApplicationStatus.Accepted.key => ApplicationStatus.Accepted
    case ApplicationStatus.AttemptingRegistration.key =>
      ApplicationStatus.AttemptingRegistration
    case ApplicationStatus.Registered.key => ApplicationStatus.Registered
    case ApplicationStatus.Complete.key   => ApplicationStatus.Complete
  }

  def unapply(arg: ApplicationStatus): Option[String] = Some(arg.key)

  implicit val applicationStatusFormat: Format[ApplicationStatus] =
    Json.format[ApplicationStatus]

  val allStatuses = Seq(Pending, Rejected, Accepted, AttemptingRegistration, Registered, Complete)
}
