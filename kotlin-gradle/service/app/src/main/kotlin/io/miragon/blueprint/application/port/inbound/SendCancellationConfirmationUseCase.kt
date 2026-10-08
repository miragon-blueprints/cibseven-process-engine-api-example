package io.miragon.blueprint.application.port.inbound

import io.miragon.blueprint.domain.leasing.ApplicationId

/** Confirms to the customer that the application was cancelled. */
fun interface SendCancellationConfirmationUseCase {
    fun sendCancellationConfirmation(id: ApplicationId)
}
