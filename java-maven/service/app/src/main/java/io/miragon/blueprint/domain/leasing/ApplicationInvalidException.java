package io.miragon.blueprint.domain.leasing;

/**
 * Raised while validating an application that cannot proceed (e.g. no income to cover the rate).
 * The inbound CIB seven adapter translates this into the BPMN error {@code applicationInvalid},
 * which the process catches on the validate task's boundary event.
 */
public final class ApplicationInvalidException extends RuntimeException {

    private final ApplicationId applicationId;
    private final String reason;

    public ApplicationInvalidException(ApplicationId applicationId, String reason) {
        super("Application " + applicationId.value() + " is invalid: " + reason);
        this.applicationId = applicationId;
        this.reason = reason;
    }

    public ApplicationId getApplicationId() {
        return applicationId;
    }

    public String getReason() {
        return reason;
    }
}
