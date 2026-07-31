package io.miragon.blueprint.adapter.inbound.cibseven

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker
import dev.bpmcrafters.processengine.worker.Variable
import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.ServiceTasks
import io.miragon.blueprint.application.port.inbound.CancelInsurancePolicyUseCase
import io.miragon.blueprint.domain.leasing.ApplicationId
import org.springframework.stereotype.Component

/** Compensation handler for the insurance policy — consumes the `cancelPolicy` external task. */
@Component
class CancelPolicyWorker(
    private val useCase: CancelInsurancePolicyUseCase,
) {

    @ProcessEngineWorker(topic = ServiceTasks.CANCEL_POLICY)
    fun cancelPolicy(@Variable applicationId: String) {
        useCase.cancelPolicy(ApplicationId.of(applicationId))
    }
}
