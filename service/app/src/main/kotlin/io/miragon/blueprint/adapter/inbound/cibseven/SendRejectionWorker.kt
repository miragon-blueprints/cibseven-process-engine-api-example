package io.miragon.blueprint.adapter.inbound.cibseven

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker
import dev.bpmcrafters.processengine.worker.Variable
import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.ServiceTasks
import io.miragon.blueprint.application.port.inbound.RejectApplicationUseCase
import io.miragon.blueprint.domain.leasing.ApplicationId
import org.springframework.stereotype.Component

@Component
class SendRejectionWorker(
    private val useCase: RejectApplicationUseCase,
) {

    @ProcessEngineWorker(topic = ServiceTasks.SEND_REJECTION)
    fun sendRejection(@Variable applicationId: String) {
        useCase.reject(ApplicationId.of(applicationId))
    }
}
