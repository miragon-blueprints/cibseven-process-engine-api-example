package io.miragon.blueprint.adapter.outbound.cibseven

import dev.bpmcrafters.processengineapi.CommonRestrictions
import dev.bpmcrafters.processengineapi.Empty
import dev.bpmcrafters.processengineapi.correlation.CorrelateMessageCmd
import dev.bpmcrafters.processengineapi.correlation.CorrelationApi
import dev.bpmcrafters.processengineapi.process.ProcessInformation
import dev.bpmcrafters.processengineapi.process.StartProcessApi
import dev.bpmcrafters.processengineapi.process.StartProcessByMessageCmd
import dev.bpmcrafters.processengineapi.task.CompleteTaskCmd
import dev.bpmcrafters.processengineapi.task.TaskInformation
import dev.bpmcrafters.processengineapi.task.UserTaskCompletionApi
import dev.bpmcrafters.processengineapi.task.support.UserTaskSupport
import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.FlowNodes
import io.miragon.blueprint.adapter.process.Messages
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.leasing.testLeasingApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.cibseven.bpm.engine.MismatchingMessageCorrelationException
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

class LeasingProcessAdapterTest {

    private val startProcessApi = mockk<StartProcessApi>()
    private val correlationApi = mockk<CorrelationApi>()
    private val userTaskSupport = mockk<UserTaskSupport>()
    private val userTaskCompletionApi = mockk<UserTaskCompletionApi>()
    private val underTest = LeasingProcessAdapter(
        startProcessApi = startProcessApi,
        correlationApi = correlationApi,
        userTaskSupport = userTaskSupport,
        userTaskCompletionApi = userTaskCompletionApi,
    )

    private val id = ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))

    @Test
    fun `submitRequest starts the process by message with the application variables and keys`() {

        // given: a leasing application and a captured start command
        val application = testLeasingApplication(id = id)
        val cmd = slot<StartProcessByMessageCmd>()
        every { startProcessApi.startProcess(capture(cmd)) } returns
            CompletableFuture.completedFuture(mockk<ProcessInformation>())

        // when: the request is submitted
        underTest.submitRequest(application)

        // then: the leasing-request message starts the process with the DMN inputs, bike, business + correlation key
        assertThat(cmd.captured.messageName).isEqualTo(Messages.MIRAVELO_LEASING_REQUEST_RECEIVED.value)
        val payload = cmd.captured.get()
        assertThat(payload["applicationId"]).isEqualTo(id.value.toString())
        assertThat(payload["age"]).isEqualTo(35)
        assertThat(payload["monthlyNetIncome"]).isEqualTo(3500.0)
        assertThat(payload["bikeId"]).isEqualTo("BIKE-900")
        assertThat(payload[CommonRestrictions.BUSINESS_KEY]).isEqualTo(id.value.toString())
        assertThat(payload[CommonRestrictions.CORRELATION_KEY]).isEqualTo(id.value.toString())
    }

    @Test
    fun `completeAlternativeClarification resolves the task by application id and completes it`() {

        // given: an open clarify-alternative task delivered to the user-task pool for this application
        val task = TaskInformation(
            taskId = "task-1",
            meta = mapOf(CommonRestrictions.ACTIVITY_ID to FlowNodes.UserTaskClarifyAlternative.id.value),
        )
        every { userTaskSupport.getAllTasks() } returns listOf(task)
        every { userTaskSupport.getPayload("task-1") } returns mapOf("applicationId" to id.value.toString())
        val cmd = slot<CompleteTaskCmd>()
        every { userTaskCompletionApi.completeTask(capture(cmd)) } returns CompletableFuture.completedFuture(Empty)

        // when: an alternative bike is selected from the outside
        underTest.completeAlternativeClarification(id, alternativeFound = true, bikeId = BikeId("BIKE-42"))

        // then: the resolved task is completed with the decision and the chosen bike
        assertThat(cmd.captured.taskId).isEqualTo("task-1")
        val payload = cmd.captured.get()
        assertThat(payload["alternativeFound"]).isEqualTo(true)
        assertThat(payload["bikeId"]).isEqualTo("BIKE-42")
    }

    @Test
    fun `completeAlternativeClarification skips a pooled task whose payload cannot be read`() {

        // given: a stale clarify-alternative task whose payload lookup fails, next to the one of this application
        every { userTaskSupport.getAllTasks() } returns
            listOf(clarifyAlternativeTask("task-stale"), clarifyAlternativeTask("task-1"))
        every { userTaskSupport.getPayload("task-stale") } throws IllegalArgumentException("unknown task")
        every { userTaskSupport.getPayload("task-1") } returns mapOf("applicationId" to id.value.toString())
        val cmd = slot<CompleteTaskCmd>()
        every { userTaskCompletionApi.completeTask(capture(cmd)) } returns CompletableFuture.completedFuture(Empty)

        // when: the clarification ends without an alternative
        underTest.completeAlternativeClarification(id, alternativeFound = false, bikeId = null)

        // then: the failing task counts as no match; the matching task is completed with the decision only
        assertThat(cmd.captured.taskId).isEqualTo("task-1")
        assertThat(cmd.captured.get()).isEqualTo(mapOf("alternativeFound" to false))
    }

    @Test
    fun `completeAlternativeClarification ignores pooled tasks of other activities and applications`() {

        // given: a task of another activity for this application and a clarify-alternative task of another
        // application, both pooled before the clarify-alternative task of this application
        val otherActivity = TaskInformation(
            taskId = "task-other-activity",
            meta = mapOf(CommonRestrictions.ACTIVITY_ID to "userTask_somethingElse"),
        )
        every { userTaskSupport.getAllTasks() } returns
            listOf(otherActivity, clarifyAlternativeTask("task-other-application"), clarifyAlternativeTask("task-1"))
        every { userTaskSupport.getPayload("task-other-activity") } returns
            mapOf("applicationId" to id.value.toString())
        every { userTaskSupport.getPayload("task-other-application") } returns
            mapOf("applicationId" to UUID.randomUUID().toString())
        every { userTaskSupport.getPayload("task-1") } returns mapOf("applicationId" to id.value.toString())
        val cmd = slot<CompleteTaskCmd>()
        every { userTaskCompletionApi.completeTask(capture(cmd)) } returns CompletableFuture.completedFuture(Empty)

        // when: an alternative bike is selected from the outside
        underTest.completeAlternativeClarification(id, alternativeFound = true, bikeId = BikeId("BIKE-42"))

        // then: only the clarify-alternative task of this application is completed
        verify(exactly = 1) { userTaskCompletionApi.completeTask(any()) }
        assertThat(cmd.captured.taskId).isEqualTo("task-1")
    }

    @Test
    fun `completeAlternativeClarification waits for the task to be delivered`() {
        val deliveredTask = TaskInformation(
            taskId = "task-1",
            meta = mapOf(CommonRestrictions.ACTIVITY_ID to FlowNodes.UserTaskClarifyAlternative.id.value),
        )
        every { userTaskSupport.getAllTasks() } returnsMany listOf(emptyList(), listOf(deliveredTask))
        every { userTaskSupport.getPayload("task-1") } returns mapOf("applicationId" to id.value.toString())
        val cmd = slot<CompleteTaskCmd>()
        every { userTaskCompletionApi.completeTask(capture(cmd)) } returns CompletableFuture.completedFuture(Empty)

        underTest.completeAlternativeClarification(id, alternativeFound = true, bikeId = null)

        assertThat(cmd.captured.taskId).isEqualTo("task-1")
        verify(exactly = 2) { userTaskSupport.getAllTasks() }
    }

    @Test
    fun `completeAlternativeClarification propagates the interruption when the task wait is interrupted`() {

        // given: no clarify-alternative task in the pool yet and an interrupted caller
        every { userTaskSupport.getAllTasks() } returns emptyList()
        Thread.currentThread().interrupt()
        try {
            // when / then: the wait aborts at once with the InterruptedException itself
            assertThatThrownBy { underTest.completeAlternativeClarification(id, alternativeFound = true, bikeId = null) }
                .isExactlyInstanceOf(InterruptedException::class.java)
            // and: the interrupt flag was consumed by the aborted wait
            assertThat(Thread.currentThread().isInterrupted).isFalse()
        } finally {
            Thread.interrupted()
        }
        verify(exactly = 1) { userTaskSupport.getAllTasks() }
        verify(exactly = 0) { userTaskCompletionApi.completeTask(any()) }
    }

    @Test
    fun `correlation rethrows an unchecked engine failure unwrapped`() {

        // given: the engine rejects the correlation, e.g. because the token already left the wait state
        val failure = MismatchingMessageCorrelationException("no execution waits for the message")
        every { correlationApi.correlateMessage(any()) } returns CompletableFuture.failedFuture(failure)

        // when / then: the very engine exception surfaces (the REST advice maps it to 409), not the async wrapper
        assertThatThrownBy { underTest.correlateContractSigned(id) }.isSameAs(failure)
    }

    @Test
    fun `correlation rethrows an error unwrapped`() {

        // given: the correlation fails with an Error
        val failure = Error("engine failure")
        every { correlationApi.correlateMessage(any()) } returns CompletableFuture.failedFuture(failure)

        // when / then: the Error itself surfaces, not the async wrapper
        assertThatThrownBy { underTest.correlateHandoverReported(id) }.isSameAs(failure)
    }

    @Test
    fun `correlation rethrows a checked cause unwrapped`() {

        // given: the correlation fails with a checked exception
        val failure = Exception("checked failure")
        every { correlationApi.correlateMessage(any()) } returns CompletableFuture.failedFuture(failure)

        // when / then: the checked exception itself surfaces, not the async wrapper
        assertThatThrownBy { underTest.correlateApplicationWithdrawn(id) }.isSameAs(failure)
    }

    @Test
    fun `a failed correlation without an engine cause surfaces the async wrapper itself`() {
        val causelessFailure = CompletionException("correlation aborted", null)
        val failingCorrelation = mockk<CompletableFuture<Empty>>()
        every { failingCorrelation.join() } throws causelessFailure
        every { correlationApi.correlateMessage(any()) } returns failingCorrelation

        assertThatThrownBy { underTest.correlateContractSigned(id) }.isSameAs(causelessFailure)
    }

    @Test
    fun `correlateContractSigned correlates the message by the global correlation key`() {
        assertCorrelation(Messages.MIRAVELO_CONTRACT_SIGNED.value) { underTest.correlateContractSigned(id) }
    }

    @Test
    fun `correlateHandoverReported correlates the message by the global correlation key`() {
        assertCorrelation(Messages.MIRAVELO_HANDOVER_REPORTED.value) { underTest.correlateHandoverReported(id) }
    }

    @Test
    fun `correlateApplicationWithdrawn correlates the message by the global correlation key`() {
        assertCorrelation(Messages.MIRAVELO_APPLICATION_WITHDRAWN.value) { underTest.correlateApplicationWithdrawn(id) }
    }

    private fun assertCorrelation(expectedMessage: String, action: () -> Unit) {

        // given: a captured correlate command
        val cmd = slot<CorrelateMessageCmd>()
        every { correlationApi.correlateMessage(capture(cmd)) } returns CompletableFuture.completedFuture(Empty)

        // when: the correlation is triggered
        action()

        // then: the expected message correlates to the instance whose global correlationKey equals the id
        verify { correlationApi.correlateMessage(any()) }
        assertThat(cmd.captured.messageName).isEqualTo(expectedMessage)
        assertThat(cmd.captured.correlation.get().correlationKey).isEqualTo(id.value.toString())
        assertThat(cmd.captured.restrictions["useGlobalCorrelationKey"]).isEqualTo("true")
    }

    private fun clarifyAlternativeTask(taskId: String) =
        TaskInformation(
            taskId = taskId,
            meta = mapOf(CommonRestrictions.ACTIVITY_ID to FlowNodes.UserTaskClarifyAlternative.id.value),
        )
}
