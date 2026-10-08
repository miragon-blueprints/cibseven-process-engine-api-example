package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.inbound.ActivateLeasingUseCase
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.domain.leasing.ApplicationId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ActivateLeasingService(
    private val repository: LeasingApplicationRepository,
) : ActivateLeasingUseCase {

    override fun activate(id: ApplicationId) {
        val application = repository.findById(id) ?: error("Unknown application $id")
        repository.save(application.activate())
    }
}
