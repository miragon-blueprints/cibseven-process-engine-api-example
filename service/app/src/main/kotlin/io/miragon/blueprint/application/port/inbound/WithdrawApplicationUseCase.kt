package io.miragon.blueprint.application.port.inbound

import io.miragon.blueprint.domain.leasing.ApplicationId

interface WithdrawApplicationUseCase {
    fun withdraw(id: ApplicationId)
}
