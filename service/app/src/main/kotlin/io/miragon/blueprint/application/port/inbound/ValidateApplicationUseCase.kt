package io.miragon.blueprint.application.port.inbound

import io.miragon.blueprint.domain.leasing.ApplicationId

interface ValidateApplicationUseCase {
    /** Validates the application; throws [io.miragon.blueprint.domain.ApplicationInvalidException] if it cannot proceed. */
    fun validate(id: ApplicationId)
}
