package io.miragon.blueprint.adapter.inbound.cibseven

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker
import dev.bpmcrafters.processengine.worker.Variable
import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.ServiceTasks
import io.miragon.blueprint.application.port.inbound.CancelContractUseCase
import io.miragon.blueprint.domain.leasing.ApplicationId
import org.springframework.stereotype.Component

/** Compensation handler for the concluded contract — consumes the `cancelContract` external task. */
@Component
class CancelContractWorker(
    private val useCase: CancelContractUseCase,
) {

    @ProcessEngineWorker(topic = ServiceTasks.CANCEL_CONTRACT)
    fun cancelContract(@Variable applicationId: String) {
        useCase.cancelContract(ApplicationId.of(applicationId))
    }
}
