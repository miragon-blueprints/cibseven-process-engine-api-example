package io.miragon.blueprint.adapter.outbound.cibseven

import dev.bpmcrafters.processengineapi.CommonRestrictions
import dev.bpmcrafters.processengineapi.correlation.CorrelateMessageCmd
import dev.bpmcrafters.processengineapi.correlation.Correlation
import dev.bpmcrafters.processengineapi.correlation.CorrelationApi
import dev.bpmcrafters.processengineapi.process.StartProcessApi
import dev.bpmcrafters.processengineapi.process.StartProcessByMessageCmd
import dev.bpmcrafters.processengineapi.task.CompleteTaskCmd
import dev.bpmcrafters.processengineapi.task.UserTaskCompletionApi
import dev.bpmcrafters.processengineapi.task.support.UserTaskSupport
import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.Elements
import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.Messages
import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.Variables
import io.miragon.blueprint.application.port.outbound.LeasingProcess
import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.LeasingApplication
import org.springframework.stereotype.Component
import java.util.concurrent.CompletionException

/**
 * Drives the embedded CIB seven engine through the process-engine-api. The application id is used as
 * both the process business key (so the `cancelBikeOrder` call activity keeps the same key) and the
 * correlation key (so later messages correlate to the right instance). The variable names come from
 * the typed process API generated from `bike-leasing.bpmn`.
 *
 * The `clarifyAlternative` user task is looked up in the [UserTaskSupport] pool (fed by the adapter's
 * user-task delivery) and then completed via the [UserTaskCompletionApi] — the process-engine-api
 * has no server-side task query, so completion always goes through a resolved task id.
 */
@Component
class LeasingProcessAdapter(
    private val startProcessApi: StartProcessApi,
    private val correlationApi: CorrelationApi,
    private val userTaskSupport: UserTaskSupport,
    private val userTaskCompletionApi: UserTaskCompletionApi,
) : LeasingProcess {

    override fun submitRequest(application: LeasingApplication) {
        val start = Variables.StartEventLeasingRequestReceived
        val key = application.id.value.toString()
        startProcessApi.startProcess(
            cmd = StartProcessByMessageCmd(
                messageName = Messages.MIRAVELO_LEASING_REQUEST_RECEIVED.value,
                payload = mapOf(
                    start.APPLICATION_ID.value to key,
                    start.BIKE_ID.value to application.bikeId.value,
                    start.MONTHLY_NET_INCOME.value to application.monthlyNetIncome,
                    start.AGE.value to application.age,
                    // Preserve the engine business key and seed the global correlation variable.
                    CommonRestrictions.BUSINESS_KEY to key,
                    CommonRestrictions.CORRELATION_KEY to key,
                ),
            ),
        ).join()
    }

    override fun correlateContractSigned(id: ApplicationId) =
        correlate(Messages.MIRAVELO_CONTRACT_SIGNED.value, id)

    override fun correlateHandoverReported(id: ApplicationId) =
        correlate(Messages.MIRAVELO_HANDOVER_REPORTED.value, id)

    override fun correlateApplicationWithdrawn(id: ApplicationId) =
        correlate(Messages.MIRAVELO_APPLICATION_WITHDRAWN.value, id)

    /**
     * Correlates [messageName] to the instance whose global `correlationKey` variable equals the id.
     * A no-longer-valid correlation (e.g. the token already left the wait state) surfaces the engine's
     * [org.cibseven.bpm.engine.MismatchingMessageCorrelationException], which the REST advice maps to a
     * 409 — so it is unwrapped from the [CompletionException] the async API wraps it in.
     */
    private fun correlate(messageName: String, id: ApplicationId) {
        try {
            correlationApi.correlateMessage(
                cmd = CorrelateMessageCmd(
                    messageName = messageName,
                    payload = emptyMap(),
                    correlation = Correlation.withKey(id.value.toString()),
                    restrictions = CommonRestrictions.builder()
                        .withRestriction("useGlobalCorrelationKey", "true")
                        .build(),
                ),
            ).join()
        } catch (e: CompletionException) {
            throw e.cause ?: e
        }
    }

    /**
     * Completes the `Clarify alternative with customer` user task via the engine client — the same
     * task a human could complete through its deployed Camunda Form in the Tasklist.
     */
    override fun completeAlternativeClarification(
        id: ApplicationId,
        alternativeFound: Boolean,
        bikeId: BikeId?,
    ) {
        val taskId = awaitClarifyAlternativeTaskId(id)
        val variables = buildMap<String, Any?> {
            put(Variables.UserTaskClarifyAlternative.ALTERNATIVE_FOUND.value, alternativeFound)
            // The re-order reads the same start-injected bike variable, so reuse its name.
            bikeId?.let { put(Variables.StartEventLeasingRequestReceived.BIKE_ID.value, it.value) }
        }
        userTaskCompletionApi.completeTask(CompleteTaskCmd(taskId, variables)).join()
    }

    /**
     * Resolves the open `clarifyAlternative` task id for [id] from the [UserTaskSupport] pool, waiting
     * briefly for the adapter's user-task delivery to hand it over. Fails loudly if none appears.
     */
    private fun awaitClarifyAlternativeTaskId(id: ApplicationId): String {
        val applicationId = id.value.toString()
        val applicationIdKey = Variables.StartEventLeasingRequestReceived.APPLICATION_ID.value
        repeat(TASK_LOOKUP_ATTEMPTS) {
            userTaskSupport.getAllTasks()
                .filter { it.meta[CommonRestrictions.ACTIVITY_ID] == Elements.USER_TASK_CLARIFY_ALTERNATIVE.value }
                .firstOrNull { task ->
                    runCatching { userTaskSupport.getPayload(task.taskId)[applicationIdKey] == applicationId }
                        .getOrDefault(false)
                }
                ?.let { return it.taskId }
            Thread.sleep(TASK_LOOKUP_INTERVAL_MS)
        }
        error("No open '${Elements.USER_TASK_CLARIFY_ALTERNATIVE.value}' task for application $applicationId")
    }

    private companion object {
        const val TASK_LOOKUP_ATTEMPTS = 50
        const val TASK_LOOKUP_INTERVAL_MS = 200L
    }
}
