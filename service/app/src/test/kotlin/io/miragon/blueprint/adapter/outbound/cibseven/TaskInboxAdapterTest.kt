package io.miragon.blueprint.adapter.outbound.cibseven

import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.Elements
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.cibseven.bpm.engine.RuntimeService
import org.cibseven.bpm.engine.TaskService
import org.cibseven.bpm.engine.runtime.ProcessInstance
import org.cibseven.bpm.engine.runtime.ProcessInstanceQuery
import org.cibseven.bpm.engine.task.Task
import org.cibseven.bpm.engine.task.TaskQuery
import org.junit.jupiter.api.Test
import java.util.Date
import java.util.UUID

class TaskInboxAdapterTest {

    private val taskService = mockk<TaskService>()
    private val runtimeService = mockk<RuntimeService>()
    private val underTest = TaskInboxAdapter(taskService, runtimeService)

    private val applicationId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000").toString()

    @Test
    fun `translates open clarify-alternative tasks into their application business keys`() {
        // given: one active clarify-alternative task on an instance keyed by the application id
        val task = mockk<Task> {
            every { processInstanceId } returns "proc-1"
            every { createTime } returns Date(0)
        }
        val taskQuery = stubTaskQuery(returns = listOf(task))
        stubProcessInstanceQuery(id = "proc-1", businessKey = applicationId)

        // when: the inbox is read
        val result = underTest.findOpenClarifications()

        // then: the open task surfaces as its application id, queried by the clarify-alternative key
        assertThat(result).hasSize(1)
        assertThat(result.single().applicationId).isEqualTo(ApplicationId.of(applicationId))
        verify { taskQuery.taskDefinitionKey(Elements.USER_TASK_CLARIFY_ALTERNATIVE.value) }
    }

    @Test
    fun `drops a task whose instance has no resolvable business key`() {
        // given: an open task whose process instance is not returned by the lookup
        val task = mockk<Task> {
            every { processInstanceId } returns "proc-missing"
            every { createTime } returns Date(0)
        }
        stubTaskQuery(returns = listOf(task))
        stubProcessInstanceQuery(id = "proc-other", businessKey = applicationId)

        // when / then: the unresolved task is skipped
        assertThat(underTest.findOpenClarifications()).isEmpty()
    }

    @Test
    fun `returns nothing and skips the instance query when no task is open`() {
        // given: no open tasks
        stubTaskQuery(returns = emptyList())

        // when / then: the result is empty; the (empty) instance lookup returns an empty map
        assertThat(underTest.findOpenClarifications()).isEmpty()
    }

    private fun stubTaskQuery(returns: List<Task>): TaskQuery {
        val query = mockk<TaskQuery>()
        every { taskService.createTaskQuery() } returns query
        every { query.taskDefinitionKey(any()) } returns query
        every { query.active() } returns query
        every { query.list() } returns returns
        return query
    }

    private fun stubProcessInstanceQuery(id: String, businessKey: String) {
        val instance = mockk<ProcessInstance> {
            every { this@mockk.id } returns id
            every { this@mockk.businessKey } returns businessKey
        }
        val query = mockk<ProcessInstanceQuery>()
        every { runtimeService.createProcessInstanceQuery() } returns query
        every { query.processInstanceIds(any()) } returns query
        every { query.list() } returns listOf(instance)
    }
}
