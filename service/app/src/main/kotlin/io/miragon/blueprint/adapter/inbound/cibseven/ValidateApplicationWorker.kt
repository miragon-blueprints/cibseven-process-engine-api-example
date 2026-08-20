package io.miragon.blueprint.adapter.inbound.cibseven

import dev.bpmcrafters.processengine.worker.BpmnErrorOccurred
import dev.bpmcrafters.processengine.worker.ProcessEngineWorker
import dev.bpmcrafters.processengine.worker.Variable
import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.Errors
import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.ServiceTasks
import io.miragon.blueprint.application.port.inbound.ValidateApplicationUseCase
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.ApplicationInvalidException
import org.springframework.stereotype.Component

/**
 * Consumes the `validateApplication` external task. An invalid application is reported to the engine
 * as the BPMN error `applicationInvalid`, which the validate task's boundary event catches.
 */
@Component
class ValidateApplicationWorker(
    private val useCase: ValidateApplicationUseCase,
) {

    @ProcessEngineWorker(topic = ServiceTasks.VALIDATE_APPLICATION)
    fun validateApplication(@Variable applicationId: String) {
        try {
            useCase.validate(ApplicationId.of(applicationId))
        } catch (e: ApplicationInvalidException) {
            throw BpmnErrorOccurred(e.reason, Errors.APPLICATION_INVALID.code, emptyMap())
        }
    }
}
