package io.miragon.blueprint.adapter.inbound.cibseven;

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker;
import dev.bpmcrafters.processengine.worker.Variable;
import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.ServiceTasks;
import io.miragon.blueprint.application.port.inbound.IssueInsurancePolicyUseCase;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import org.springframework.stereotype.Component;

@Component
public class IssueInsurancePolicyWorker {

    private final IssueInsurancePolicyUseCase useCase;

    public IssueInsurancePolicyWorker(IssueInsurancePolicyUseCase useCase) {
        this.useCase = useCase;
    }

    @ProcessEngineWorker(topic = ServiceTasks.ISSUE_INSURANCE_POLICY)
    public void issueInsurancePolicy(@Variable String applicationId) {
        useCase.issuePolicy(ApplicationId.of(applicationId));
    }
}
