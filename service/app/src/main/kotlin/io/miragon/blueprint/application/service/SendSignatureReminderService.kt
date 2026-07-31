package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.inbound.SendSignatureReminderUseCase
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.application.port.outbound.NotificationPort
import io.miragon.blueprint.domain.leasing.ApplicationId
import org.springframework.stereotype.Service

@Service
class SendSignatureReminderService(
    private val repository: LeasingApplicationRepository,
    private val notification: NotificationPort,
) : SendSignatureReminderUseCase {

    override fun sendSignatureReminder(id: ApplicationId) {
        val application = repository.findById(id) ?: error("Unknown application $id")
        notification.send("Reminder: your leasing contract is still awaiting signature", application)
    }
}
