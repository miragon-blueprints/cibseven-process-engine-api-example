package io.miragon.blueprint.process.model

import io.miragon.bpmn.domain.shared.ServiceTaskDefinition
import io.miragon.bpmn.domain.validation.SingleModelValidationRule
import io.miragon.bpmn.domain.validation.model.Severity
import io.miragon.bpmn.domain.validation.model.SingleModelValidationContext
import io.miragon.bpmn.domain.validation.model.ValidationViolation

/**
 * Custom bpmn-to-code validation rule: every *implemented* service task must be an external task
 * (`camunda:type="external"` with a topic) — i.e. no delegate expression, `camunda:class` or plain
 * `${...}` expressions.
 *
 * This keeps all service-task logic behind process-engine-api `@ProcessEngineWorker` beans that
 * consume the external tasks, matching this blueprint's inbound-adapter design. Service tasks with no
 * implementation at all are left to the built-in `MISSING_SERVICE_TASK_IMPLEMENTATION` rule.
 */
class ServiceTaskExternalTopicRule : SingleModelValidationRule {

    override val id: String = "SERVICE_TASK_MUST_USE_EXTERNAL_TOPIC"

    override val severity: Severity = Severity.ERROR

    override fun validate(context: SingleModelValidationContext): List<ValidationViolation> =
        context.model.serviceTasks
            .filter { it.hasImplementation() && !usesExternalTask(it) }
            .map { task ->
                ValidationViolation(
                    ruleId = id,
                    severity = severity,
                    elementId = task.id,
                    processId = context.model.processId,
                    message = "Service task '${task.id}' must be an external task with a topic",
                )
            }

    private fun usesExternalTask(task: ServiceTaskDefinition): Boolean {
        val kind = task.engineSpecificProperties[ServiceTaskDefinition.IMPL_KIND_KEY] as? String
        return kind == EXTERNAL_TASK_KIND
    }

    private companion object {
        const val EXTERNAL_TASK_KIND = "EXTERNAL_TASK"
    }
}
