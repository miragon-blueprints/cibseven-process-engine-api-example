package io.miragon.blueprint.adapter.inbound.cibseven

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker
import dev.bpmcrafters.processengine.worker.Variable
import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.ServiceTasks
import io.miragon.blueprint.application.port.inbound.SendCancellationConfirmationUseCase
import io.miragon.blueprint.domain.leasing.ApplicationId
import org.springframework.stereotype.Component

@Component
class SendCancellationConfirmationWorker(
    private val useCase: SendCancellationConfirmationUseCase,
) {

    @ProcessEngineWorker(topic = ServiceTasks.SEND_CANCELLATION_CONFIRMATION)
    fun sendCancellationConfirmation(@Variable applicationId: String) {
        useCase.sendCancellationConfirmation(ApplicationId.of(applicationId))
    }
}
