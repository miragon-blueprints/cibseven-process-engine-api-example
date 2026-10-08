package io.miragon.blueprint.adapter.inbound.cibseven;

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker;
import dev.bpmcrafters.processengine.worker.Variable;
import io.miragon.blueprint.adapter.process.ServiceTasks;
import io.miragon.blueprint.application.port.inbound.SendCancellationConfirmationUseCase;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import org.springframework.stereotype.Component;

@Component
public class SendCancellationConfirmationWorker {

    private final SendCancellationConfirmationUseCase useCase;

    public SendCancellationConfirmationWorker(SendCancellationConfirmationUseCase useCase) {
        this.useCase = useCase;
    }

    @ProcessEngineWorker(topic = ServiceTasks.SEND_CANCELLATION_CONFIRMATION)
    public void sendCancellationConfirmation(@Variable String applicationId) {
        useCase.sendCancellationConfirmation(ApplicationId.of(applicationId));
    }
}
