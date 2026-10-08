package io.miragon.blueprint.application.port.inbound

import io.miragon.blueprint.domain.leasing.ApplicationId

/** Compensation: revokes an insurance policy that was already issued. */
fun interface CancelInsurancePolicyUseCase {
    fun cancelPolicy(id: ApplicationId)
}
