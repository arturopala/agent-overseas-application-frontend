package uk.gov.hmrc.agentoverseasapplicationfrontend.models

import play.api.mvc.{Request, WrappedRequest}

case class CredentialRequest[A](authProviderId: String, request: Request[A], agentSession: AgentSession)
    extends WrappedRequest[A](request)
