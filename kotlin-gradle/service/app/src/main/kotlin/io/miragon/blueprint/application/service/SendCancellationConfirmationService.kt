package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.inbound.SendCancellationConfirmationUseCase
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.application.port.outbound.NotificationPort
import io.miragon.blueprint.domain.leasing.ApplicationId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Final step of the cancellation path (serviceTask_sendCancellationConfirmation →
 * endEvent_applicationCancelled): confirm to the customer AND move the application to CANCELLED, so
 * the read model reflects the terminal state (symmetric with the reject path). Without this the
 * process would end while the REST/UI status still read ORDERED.
 */
@Service
@Transactional
class SendCancellationConfirmationService(
    private val repository: LeasingApplicationRepository,
    private val notification: NotificationPort,
) : SendCancellationConfirmationUseCase {

    override fun sendCancellationConfirmation(id: ApplicationId) {
        val application = repository.findById(id) ?: error("Unknown application $id")
        notification.send("Your bike-leasing application has been cancelled", application)
        repository.save(application.cancel())
    }
}
