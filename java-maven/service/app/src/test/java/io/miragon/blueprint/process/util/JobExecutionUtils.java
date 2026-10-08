package io.miragon.blueprint.process.util;

import java.util.List;
import org.cibseven.bpm.engine.ManagementService;
import org.cibseven.bpm.engine.ProcessEngine;
import org.cibseven.bpm.engine.runtime.Job;

/**
 * Process-test helper for driving the engine while the job executor is disabled.
 */
public final class JobExecutionUtils {

    private static final long DEFAULT_TIMEOUT_MILLIS = 15_000;

    private JobExecutionUtils() {
    }

    /**
     * Drives the process to its next wait state, giving up after 15 seconds. See
     * {@link #continueToNextWaitState(ProcessEngine, long)}.
     */
    public static void continueToNextWaitState(ProcessEngine processEngine) throws InterruptedException {
        continueToNextWaitState(processEngine, DEFAULT_TIMEOUT_MILLIS);
    }

    /**
     * Drives the process to its next wait state. Necessary because the job executor is disabled in tests
     * for determinism.
     *
     * <p>Unlike the classic-delegate blueprint, this process-engine-api variant runs its service tasks as
     * external tasks consumed by the real, asynchronously polling {@code @ProcessEngineWorker} beans. So this
     * helper both executes parked async-continuation ({@code camunda:asyncAfter}) message jobs synchronously
     * <em>and</em> gives the polling workers time to pick up and complete any open external tasks — settling
     * once no message job and no external task remains for a few consecutive checks.
     */
    public static void continueToNextWaitState(ProcessEngine processEngine, long timeoutMillis)
        throws InterruptedException {
        ManagementService managementService = processEngine.getManagementService();
        long deadline = System.currentTimeMillis() + timeoutMillis;
        int idleIterations = 0;
        while (System.currentTimeMillis() < deadline) {
            List<Job> jobs = managementService.createJobQuery()
                .active()
                .messages()
                .listPage(0, 1);
            if (!jobs.isEmpty()) {
                managementService.executeJob(jobs.get(0).getId());
                idleIterations = 0;
                continue;
            }
            if (processEngine.getExternalTaskService().createExternalTaskQuery().count() == 0L) {
                if (++idleIterations >= 3) {
                    return;
                }
            } else {
                idleIterations = 0;
            }
            Thread.sleep(200);
        }
    }
}
