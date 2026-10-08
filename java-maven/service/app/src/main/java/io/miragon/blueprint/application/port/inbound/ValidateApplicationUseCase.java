package io.miragon.blueprint.application.port.inbound;

import io.miragon.blueprint.domain.leasing.ApplicationId;

public interface ValidateApplicationUseCase {

    /**
     * Validates the application; throws {@link io.miragon.blueprint.domain.leasing.ApplicationInvalidException}
     * if it cannot proceed.
     */
    void validate(ApplicationId id);
}
