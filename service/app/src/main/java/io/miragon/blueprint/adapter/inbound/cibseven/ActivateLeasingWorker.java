package io.miragon.blueprint.adapter.inbound.cibseven;

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker;
import dev.bpmcrafters.processengine.worker.Variable;
import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.ServiceTasks;
import io.miragon.blueprint.application.port.inbound.ActivateLeasingUseCase;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import org.springframework.stereotype.Component;

/**
 * Consumes the {@code activateLeasing} external task, reached once the withdrawal period has elapsed, and
 * flips the read model to ACTIVE. Modelled as an external service task (not an engine-side listener /
 * delegate) so the activation runs through the domain like every other step — see
 * {@code docs/execution-and-task-listeners.md}.
 */
@Component
public class ActivateLeasingWorker {

    private final ActivateLeasingUseCase useCase;

    public ActivateLeasingWorker(ActivateLeasingUseCase useCase) {
        this.useCase = useCase;
    }

    @ProcessEngineWorker(topic = ServiceTasks.ACTIVATE_LEASING)
    public void activateLeasing(@Variable String applicationId) {
        useCase.activate(ApplicationId.of(applicationId));
    }
}
