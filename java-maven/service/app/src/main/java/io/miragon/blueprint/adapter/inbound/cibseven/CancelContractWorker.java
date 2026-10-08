package io.miragon.blueprint.adapter.inbound.cibseven;

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker;
import dev.bpmcrafters.processengine.worker.Variable;
import io.miragon.blueprint.adapter.process.ServiceTasks;
import io.miragon.blueprint.application.port.inbound.CancelContractUseCase;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import org.springframework.stereotype.Component;

/** Compensation handler for the concluded contract — consumes the {@code cancelContract} external task. */
@Component
public class CancelContractWorker {

    private final CancelContractUseCase useCase;

    public CancelContractWorker(CancelContractUseCase useCase) {
        this.useCase = useCase;
    }

    @ProcessEngineWorker(topic = ServiceTasks.CANCEL_CONTRACT)
    public void cancelContract(@Variable String applicationId) {
        useCase.cancelContract(ApplicationId.of(applicationId));
    }
}
