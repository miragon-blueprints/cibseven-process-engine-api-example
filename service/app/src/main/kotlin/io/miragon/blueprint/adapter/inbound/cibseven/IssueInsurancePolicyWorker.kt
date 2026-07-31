package io.miragon.blueprint.adapter.inbound.cibseven

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker
import dev.bpmcrafters.processengine.worker.Variable
import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.ServiceTasks
import io.miragon.blueprint.application.port.inbound.IssueInsurancePolicyUseCase
import io.miragon.blueprint.domain.leasing.ApplicationId
import org.springframework.stereotype.Component

@Component
class IssueInsurancePolicyWorker(
    private val useCase: IssueInsurancePolicyUseCase,
) {

    @ProcessEngineWorker(topic = ServiceTasks.ISSUE_INSURANCE_POLICY)
    fun issueInsurancePolicy(@Variable applicationId: String) {
        useCase.issuePolicy(ApplicationId.of(applicationId))
    }
}
