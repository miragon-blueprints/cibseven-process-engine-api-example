package io.miragon.blueprint.adapter.outbound.cibseven

import org.cibseven.bpm.engine.RuntimeService
import org.cibseven.bpm.engine.TaskService
import org.cibseven.bpm.engine.task.Task

/**
 * Small read-only extension helpers over the CIB seven services used by [TaskInboxAdapter], so the
 * adapter reads as intent instead of fluent query boilerplate. Both key on the process business key,
 * which this service sets to the application id.
 *
 * Only the task-inbox *reads* live here; message correlation and user-task completion go through the
 * process-engine-api in [LeasingProcessAdapter].
 */

/** All currently-active tasks of the given [taskDefinitionKey], across every process instance. */
fun TaskService.findOpenTasks(taskDefinitionKey: String): List<Task> =
    createTaskQuery()
        .taskDefinitionKey(taskDefinitionKey)
        .active()
        .list()

/**
 * Maps the given process-instance ids to their business keys in one query. Returns an empty map for
 * an empty input so callers don't issue a pointless query.
 */
fun RuntimeService.businessKeysById(processInstanceIds: Collection<String>): Map<String, String> {
    if (processInstanceIds.isEmpty()) return emptyMap()
    return createProcessInstanceQuery()
        .processInstanceIds(processInstanceIds.toSet())
        .list()
        .associate { it.id to it.businessKey }
}
