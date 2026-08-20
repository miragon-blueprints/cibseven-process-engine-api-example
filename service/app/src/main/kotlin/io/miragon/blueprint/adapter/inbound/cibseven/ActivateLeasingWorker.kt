package io.miragon.blueprint.adapter.inbound.cibseven

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker
import dev.bpmcrafters.processengine.worker.Variable
import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.ServiceTasks
import io.miragon.blueprint.application.port.inbound.ActivateLeasingUseCase
import io.miragon.blueprint.domain.leasing.ApplicationId
import org.springframework.stereotype.Component

/**
 * Consumes the `activateLeasing` external task, reached once the withdrawal period has elapsed, and
 * flips the read model to ACTIVE. Modelled as an external service task (not an engine-side listener /
 * delegate) so the activation runs through the domain like every other step — see
 * `docs/execution-and-task-listeners.md`.
 */
@Component
class ActivateLeasingWorker(
    private val useCase: ActivateLeasingUseCase,
) {

    @ProcessEngineWorker(topic = ServiceTasks.ACTIVATE_LEASING)
    fun activateLeasing(@Variable applicationId: String) {
        useCase.activate(ApplicationId.of(applicationId))
    }
}
