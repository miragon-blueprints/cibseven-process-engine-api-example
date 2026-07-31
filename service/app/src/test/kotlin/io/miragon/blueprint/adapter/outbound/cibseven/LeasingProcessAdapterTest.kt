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
import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.Elements
import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.Messages
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.leasing.testLeasingApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.CompletableFuture

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
            meta = mapOf(CommonRestrictions.ACTIVITY_ID to Elements.USER_TASK_CLARIFY_ALTERNATIVE.value),
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
}
