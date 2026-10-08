package io.miragon.blueprint.process.util;

import io.miragon.bpmn.runtime.FlowNode;
import org.cibseven.bpm.engine.ManagementService;
import org.cibseven.bpm.engine.ProcessEngine;
import org.cibseven.bpm.engine.runtime.Job;

/**
 * Process-test helper for timer events.
 */
public final class TimerUtils {

    private TimerUtils() {
    }

    /**
     * Fires the timer job of the given boundary/catch event directly, regardless of its due date.
     * Replaces clock manipulation: the tests verify that the timer path is wired correctly, not the
     * real-world waiting duration.
     */
    public static void fireTimer(ProcessEngine processEngine, FlowNode timerEvent) {
        ManagementService managementService = processEngine.getManagementService();
        Job timer =
            managementService
                .createJobQuery()
                .timers()
                .activityId(timerEvent.getId().getValue())
                .singleResult();
        if (timer == null) {
            throw new IllegalArgumentException(
                "no timer job found for activity '" + timerEvent.getId().getValue() + "'");
        }
        managementService.executeJob(timer.getId());
    }
}
