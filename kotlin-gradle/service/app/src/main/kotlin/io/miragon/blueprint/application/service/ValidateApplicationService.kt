package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.inbound.ValidateApplicationUseCase
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.domain.leasing.ApplicationId
import org.springframework.stereotype.Service

@Service
class ValidateApplicationService(
    private val repository: LeasingApplicationRepository,
) : ValidateApplicationUseCase {

    override fun validate(id: ApplicationId) {
        val application = repository.findById(id) ?: error("Unknown application $id")
        application.validate()
    }
}
