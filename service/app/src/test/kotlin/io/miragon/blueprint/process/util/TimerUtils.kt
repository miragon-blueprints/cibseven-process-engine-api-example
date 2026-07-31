package io.miragon.blueprint.process.util

import io.miragon.bpmn.runtime.ElementId
import org.cibseven.bpm.engine.ProcessEngine

/**
 * Fires the timer job of the given boundary/catch event directly, regardless of its due date.
 * Replaces clock manipulation: the tests verify that the timer path is wired correctly, not the
 * real-world waiting duration.
 */
fun ProcessEngine.fireTimer(timerActivityId: ElementId) {
    val timer =
        managementService
            .createJobQuery()
            .timers()
            .activityId(timerActivityId.value)
            .singleResult()
    requireNotNull(timer) { "no timer job found for activity '${timerActivityId.value}'" }
    managementService.executeJob(timer.id)
}
