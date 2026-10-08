package io.miragon.blueprint.process.util

import org.cibseven.bpm.engine.ProcessEngine

/**
 * Drives the process to its next wait state. Necessary because the job executor is disabled in tests
 * for determinism.
 *
 * Unlike the classic-delegate blueprint, this process-engine-api variant runs its service tasks as
 * external tasks consumed by the real, asynchronously polling `@ProcessEngineWorker` beans. So this
 * helper both executes parked async-continuation (`camunda:asyncAfter`) message jobs synchronously
 * *and* gives the polling workers time to pick up and complete any open external tasks — settling
 * once no message job and no external task remains for a few consecutive checks.
 */
fun ProcessEngine.continueToNextWaitState(timeoutMillis: Long = 15_000) {
    val deadline = System.currentTimeMillis() + timeoutMillis
    var idleIterations = 0
    while (System.currentTimeMillis() < deadline) {
        val job = managementService.createJobQuery()
            .active()
            .messages()
            .listPage(0, 1)
            .firstOrNull()
        if (job != null) {
            managementService.executeJob(job.id)
            idleIterations = 0
            continue
        }
        if (externalTaskService.createExternalTaskQuery().count() == 0L) {
            if (++idleIterations >= 3) return
        } else {
            idleIterations = 0
        }
        Thread.sleep(200)
    }
}
