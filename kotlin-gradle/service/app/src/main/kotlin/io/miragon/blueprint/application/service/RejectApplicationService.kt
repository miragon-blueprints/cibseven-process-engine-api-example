package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.inbound.RejectApplicationUseCase
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.application.port.outbound.NotificationPort
import io.miragon.blueprint.domain.leasing.ApplicationId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RejectApplicationService(
    private val repository: LeasingApplicationRepository,
    private val notification: NotificationPort,
) : RejectApplicationUseCase {

    override fun reject(id: ApplicationId) {
        val application = repository.findById(id) ?: error("Unknown application $id")
        notification.send("Your bike-leasing application was rejected", application)
        repository.save(application.reject())
    }
}
