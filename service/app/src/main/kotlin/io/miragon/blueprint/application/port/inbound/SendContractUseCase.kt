package io.miragon.blueprint.application.port.inbound

import io.miragon.blueprint.domain.leasing.ApplicationId

/** Sends the leasing contract to the customer for signature. */
fun interface SendContractUseCase {
    fun sendContract(id: ApplicationId)
}
