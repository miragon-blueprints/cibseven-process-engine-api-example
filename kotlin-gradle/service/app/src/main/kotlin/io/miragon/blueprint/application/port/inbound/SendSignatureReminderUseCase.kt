package io.miragon.blueprint.application.port.inbound

import io.miragon.blueprint.domain.leasing.ApplicationId

/** Reminds the customer that the contract is still awaiting signature. */
fun interface SendSignatureReminderUseCase {
    fun sendSignatureReminder(id: ApplicationId)
}
