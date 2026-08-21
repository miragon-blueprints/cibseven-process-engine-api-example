package io.miragon.blueprint.adapter.outbound.cibseven

import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.Elements
import io.miragon.blueprint.application.port.outbound.TaskInboxPort
import io.miragon.blueprint.domain.leasing.ApplicationId
import org.cibseven.bpm.engine.RuntimeService
import org.cibseven.bpm.engine.TaskService
import org.springframework.stereotype.Component
import java.time.ZoneId

/**
 * Reads the open `Clarify alternative with customer` tasks from the engine's task list and translates
 * them into the domain's business key (the application id). It never leaks an engine task id upward:
 * the inbox lists cases, and cases are resolved through the domain, correlated by id.
 *
 * This is a read-only query at the engine boundary. The process-engine-api has no server-side task
 * query, so the inbox reads the embedded CIB seven `TaskService` directly — the native interfaces that
 * remain available alongside the API (see `docs/execution-and-task-listeners.md`). Task *completion*
 * still goes through the process-engine-api in [LeasingProcessAdapter].
 */
@Component
class TaskInboxAdapter(
    private val taskService: TaskService,
    private val runtimeService: RuntimeService,
) : TaskInboxPort {

    override fun findOpenClarifications(): List<TaskInboxPort.OpenClarification> {
        val tasks = taskService.findOpenTasks(Elements.USER_TASK_CLARIFY_ALTERNATIVE.value)
        val businessKeys = runtimeService.businessKeysById(tasks.map { it.processInstanceId })
        return tasks.mapNotNull { task ->
            val applicationId = businessKeys[task.processInstanceId] ?: return@mapNotNull null
            TaskInboxPort.OpenClarification(
                applicationId = ApplicationId.of(applicationId),
                waitingSince = task.createTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(),
            )
        }
    }
}
