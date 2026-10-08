package io.miragon.blueprint.adapter.outbound.cibseven;

import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.FlowNodes;
import io.miragon.blueprint.application.port.outbound.TaskInboxPort;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.cibseven.bpm.engine.RuntimeService;
import org.cibseven.bpm.engine.TaskService;
import org.cibseven.bpm.engine.runtime.ProcessInstance;
import org.cibseven.bpm.engine.task.Task;
import org.springframework.stereotype.Component;

/**
 * Reads the open {@code Clarify alternative with customer} tasks from the engine's task list and translates
 * them into the domain's business key (the application id). It never leaks an engine task id upward:
 * the inbox lists cases, and cases are resolved through the domain, correlated by id.
 *
 * <p>This is a read-only query at the engine boundary. The process-engine-api has no server-side task
 * query, so the inbox reads the embedded CIB seven {@code TaskService} directly — the native interfaces that
 * remain available alongside the API (see {@code docs/execution-and-task-listeners.md}). Task
 * <em>completion</em> still goes through the process-engine-api in {@link LeasingProcessAdapter}.
 *
 * <p>The two private read-only helpers over the CIB seven services ({@link #findOpenTasks} and
 * {@link #businessKeysById}) keep the adapter reading as intent instead of fluent query boilerplate. Both
 * key on the process business key, which this service sets to the application id. Only the task-inbox
 * <em>reads</em> live here; message correlation and user-task completion go through the process-engine-api
 * in {@link LeasingProcessAdapter}.
 */
@Component
public class TaskInboxAdapter implements TaskInboxPort {

    private final TaskService taskService;
    private final RuntimeService runtimeService;

    public TaskInboxAdapter(TaskService taskService, RuntimeService runtimeService) {
        this.taskService = taskService;
        this.runtimeService = runtimeService;
    }

    @Override
    public List<TaskInboxPort.OpenClarification> findOpenClarifications() {
        List<Task> tasks = findOpenTasks(FlowNodes.UserTaskClarifyAlternative.ELEMENT_ID);
        Map<String, String> businessKeys =
                businessKeysById(tasks.stream().map(Task::getProcessInstanceId).toList());
        List<TaskInboxPort.OpenClarification> clarifications = new ArrayList<>();
        for (Task task : tasks) {
            String applicationId = businessKeys.get(task.getProcessInstanceId());
            if (applicationId == null) {
                continue;
            }
            clarifications.add(new TaskInboxPort.OpenClarification(
                    ApplicationId.of(applicationId),
                    task.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()));
        }
        return clarifications;
    }

    /** All currently-active tasks of the given {@code taskDefinitionKey}, across every process instance. */
    private List<Task> findOpenTasks(String taskDefinitionKey) {
        return taskService.createTaskQuery()
                .taskDefinitionKey(taskDefinitionKey)
                .active()
                .list();
    }

    /**
     * Maps the given process-instance ids to their business keys in one query. Returns an empty map for
     * an empty input so callers don't issue a pointless query.
     */
    private Map<String, String> businessKeysById(Collection<String> processInstanceIds) {
        if (processInstanceIds.isEmpty()) {
            return Map.of();
        }
        Map<String, String> businessKeys = new LinkedHashMap<>();
        for (ProcessInstance instance : runtimeService.createProcessInstanceQuery()
                .processInstanceIds(new LinkedHashSet<>(processInstanceIds))
                .list()) {
            businessKeys.put(instance.getId(), instance.getBusinessKey());
        }
        return businessKeys;
    }
}
