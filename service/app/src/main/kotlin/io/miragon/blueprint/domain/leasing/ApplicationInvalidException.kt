package io.miragon.blueprint.domain.leasing

/**
 * Raised while validating an application that cannot proceed (e.g. no income to cover the rate).
 * The inbound CIB seven adapter translates this into the BPMN error `applicationInvalid`,
 * which the process catches on the validate task's boundary event.
 */
class ApplicationInvalidException(
    val applicationId: ApplicationId,
    val reason: String,
) : RuntimeException("Application ${applicationId.value} is invalid: $reason")
