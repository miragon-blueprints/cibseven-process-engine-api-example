package io.miragon.blueprint.process.model;

import io.miragon.bpmn.domain.shared.ServiceTaskDefinition;
import io.miragon.bpmn.domain.shared.TaskImplementation;
import io.miragon.bpmn.domain.validation.SingleModelValidationRule;
import io.miragon.bpmn.domain.validation.model.Severity;
import io.miragon.bpmn.domain.validation.model.SingleModelValidationContext;
import io.miragon.bpmn.domain.validation.model.ValidationViolation;
import java.util.List;

/**
 * Custom bpmn-to-code validation rule: every <em>implemented</em> service task must be an external task
 * ({@code camunda:type="external"} with a topic) — i.e. no delegate expression, {@code camunda:class} or plain
 * {@code ${...}} expressions.
 *
 * <p>This keeps all service-task logic behind process-engine-api {@code @ProcessEngineWorker} beans that
 * consume the external tasks, matching this blueprint's inbound-adapter design. Service tasks with no
 * implementation at all are left to the built-in {@code MISSING_SERVICE_TASK_IMPLEMENTATION} rule.
 */
public final class ServiceTaskExternalTopicRule implements SingleModelValidationRule {

    @Override
    public String getId() {
        return "SERVICE_TASK_MUST_USE_EXTERNAL_TOPIC";
    }

    @Override
    public Severity getSeverity() {
        return Severity.ERROR;
    }

    @Override
    public List<ValidationViolation> validate(SingleModelValidationContext context) {
        return context.getModel().getServiceTasks().stream()
            .filter(task -> task.hasImplementation() && !usesExternalTask(task))
            .map(task -> new ValidationViolation(
                getId(),
                getSeverity(),
                task.getId(),
                context.getModel().getProcessId(),
                "Service task '" + task.getId() + "' must be an external task with a topic"))
            .toList();
    }

    private static boolean usesExternalTask(ServiceTaskDefinition task) {
        return task.getImplementation() instanceof TaskImplementation.ExternalTask;
    }
}
