package io.miragon.blueprint.adapter.inbound.cibseven

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker
import dev.bpmcrafters.processengine.worker.Variable
import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.ServiceTasks
import io.miragon.blueprint.application.port.inbound.SendContractUseCase
import io.miragon.blueprint.domain.leasing.ApplicationId
import org.springframework.stereotype.Component

@Component
class SendContractWorker(
    private val useCase: SendContractUseCase,
) {

    @ProcessEngineWorker(topic = ServiceTasks.SEND_CONTRACT)
    fun sendContract(@Variable applicationId: String) {
        useCase.sendContract(ApplicationId.of(applicationId))
    }
}
