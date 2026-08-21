package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.inbound.WithdrawApplicationUseCase
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.application.port.outbound.LeasingProcess
import io.miragon.blueprint.domain.leasing.ApplicationId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class WithdrawApplicationService(
    private val process: LeasingProcess,
    private val repository: LeasingApplicationRepository,
) : WithdrawApplicationUseCase {

    /**
     * Correlates the withdrawal and immediately moves the read model to WITHDRAWN, so the UI shows
     * the state change right away. The actual cancellation runs asynchronously in the process (it may
     * park on a return-clarification task) and reaches the terminal CANCELLED only once it completes.
     */
    override fun withdraw(id: ApplicationId) {
        process.correlateApplicationWithdrawn(id)
        val application = repository.findById(id) ?: error("Unknown application $id")
        repository.save(application.withdraw())
    }
}
