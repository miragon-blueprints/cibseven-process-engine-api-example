package io.miragon.blueprint.adapter.inbound.cibseven;

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker;
import dev.bpmcrafters.processengine.worker.Variable;
import io.miragon.blueprint.adapter.process.ServiceTasks;
import io.miragon.blueprint.application.port.inbound.CancelInsurancePolicyUseCase;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import org.springframework.stereotype.Component;

/** Compensation handler for the insurance policy — consumes the {@code cancelPolicy} external task. */
@Component
public class CancelPolicyWorker {

    private final CancelInsurancePolicyUseCase useCase;

    public CancelPolicyWorker(CancelInsurancePolicyUseCase useCase) {
        this.useCase = useCase;
    }

    @ProcessEngineWorker(topic = ServiceTasks.CANCEL_POLICY)
    public void cancelPolicy(@Variable String applicationId) {
        useCase.cancelPolicy(ApplicationId.of(applicationId));
    }
}
