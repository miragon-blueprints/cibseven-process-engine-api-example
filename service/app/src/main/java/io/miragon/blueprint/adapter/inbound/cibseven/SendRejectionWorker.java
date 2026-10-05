package io.miragon.blueprint.adapter.inbound.cibseven;

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker;
import dev.bpmcrafters.processengine.worker.Variable;
import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.ServiceTasks;
import io.miragon.blueprint.application.port.inbound.RejectApplicationUseCase;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import org.springframework.stereotype.Component;

@Component
public class SendRejectionWorker {

    private final RejectApplicationUseCase useCase;

    public SendRejectionWorker(RejectApplicationUseCase useCase) {
        this.useCase = useCase;
    }

    @ProcessEngineWorker(topic = ServiceTasks.SEND_REJECTION)
    public void sendRejection(@Variable String applicationId) {
        useCase.reject(ApplicationId.of(applicationId));
    }
}
