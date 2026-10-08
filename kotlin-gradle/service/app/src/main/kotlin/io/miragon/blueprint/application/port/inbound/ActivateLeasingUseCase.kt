package io.miragon.blueprint.application.port.inbound

import io.miragon.blueprint.domain.leasing.ApplicationId

interface ActivateLeasingUseCase {
    fun activate(id: ApplicationId)
}
