package io.miragon.blueprint.adapter.inbound.cibseven;

import dev.bpmcrafters.processengine.worker.BpmnErrorOccurred;
import dev.bpmcrafters.processengine.worker.ProcessEngineWorker;
import dev.bpmcrafters.processengine.worker.Variable;
import io.miragon.blueprint.adapter.process.Errors;
import io.miragon.blueprint.adapter.process.ServiceTasks;
import io.miragon.blueprint.application.port.inbound.ValidateApplicationUseCase;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import io.miragon.blueprint.domain.leasing.ApplicationInvalidException;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Consumes the {@code validateApplication} external task. An invalid application is reported to the engine
 * as the BPMN error {@code applicationInvalid}, which the validate task's boundary event catches.
 */
@Component
public class ValidateApplicationWorker {

    private final ValidateApplicationUseCase useCase;

    public ValidateApplicationWorker(ValidateApplicationUseCase useCase) {
        this.useCase = useCase;
    }

    @ProcessEngineWorker(topic = ServiceTasks.VALIDATE_APPLICATION)
    public void validateApplication(@Variable String applicationId) throws BpmnErrorOccurred {
        try {
            useCase.validate(ApplicationId.of(applicationId));
        } catch (ApplicationInvalidException e) {
            throw new BpmnErrorOccurred(e.getReason(), Errors.APPLICATION_INVALID.getCode(), Map.of());
        }
    }
}
