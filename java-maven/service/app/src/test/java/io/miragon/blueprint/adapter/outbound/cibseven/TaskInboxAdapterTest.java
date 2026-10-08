package io.miragon.blueprint.adapter.outbound.cibseven;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.FlowNodes;
import io.miragon.blueprint.application.port.outbound.TaskInboxPort;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.cibseven.bpm.engine.RuntimeService;
import org.cibseven.bpm.engine.TaskService;
import org.cibseven.bpm.engine.runtime.ProcessInstance;
import org.cibseven.bpm.engine.runtime.ProcessInstanceQuery;
import org.cibseven.bpm.engine.task.Task;
import org.cibseven.bpm.engine.task.TaskQuery;
import org.junit.jupiter.api.Test;

class TaskInboxAdapterTest {

    private final TaskService taskService = mock(TaskService.class);
    private final RuntimeService runtimeService = mock(RuntimeService.class);
    private final TaskInboxAdapter underTest = new TaskInboxAdapter(taskService, runtimeService);

    private final String applicationId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000").toString();

    @Test
    void translates_open_clarify_alternative_tasks_into_their_application_business_keys() {
        // given: one active clarify-alternative task on an instance keyed by the application id
        Task task = task("proc-1", new Date(0));
        TaskQuery taskQuery = stubTaskQuery(List.of(task));
        ProcessInstanceQuery instanceQuery = stubProcessInstanceQuery("proc-1", applicationId);

        // when: the inbox is read
        List<TaskInboxPort.OpenClarification> result = underTest.findOpenClarifications();

        // then: the open task surfaces as its application id, queried by the clarify-alternative key
        assertThat(result).hasSize(1);
        assertThat(result.get(0).applicationId()).isEqualTo(ApplicationId.of(applicationId));
        assertThat(result.get(0).waitingSince())
                .isEqualTo(LocalDateTime.ofInstant(Instant.EPOCH, ZoneId.systemDefault()));
        verify(taskQuery).taskDefinitionKey(FlowNodes.UserTaskClarifyAlternative.ELEMENT_ID);
        verify(instanceQuery).processInstanceIds(Set.of("proc-1"));
    }

    @Test
    void drops_a_task_whose_instance_has_no_resolvable_business_key() {
        // given: an open task whose process instance is not returned by the lookup
        Task task = task("proc-missing", new Date(0));
        stubTaskQuery(List.of(task));
        stubProcessInstanceQuery("proc-other", applicationId);

        // when / then: the unresolved task is skipped
        assertThat(underTest.findOpenClarifications()).isEmpty();
    }

    @Test
    void returns_nothing_and_skips_the_instance_query_when_no_task_is_open() {
        // given: no open tasks
        stubTaskQuery(List.of());

        // when / then: the result is empty; the (empty) instance lookup returns an empty map
        assertThat(underTest.findOpenClarifications()).isEmpty();
        // and: no process-instance query is issued at all
        verifyNoInteractions(runtimeService);
    }

    private Task task(String processInstanceId, Date createTime) {
        Task task = mock(Task.class);
        when(task.getProcessInstanceId()).thenReturn(processInstanceId);
        when(task.getCreateTime()).thenReturn(createTime);
        return task;
    }

    private TaskQuery stubTaskQuery(List<Task> returns) {
        TaskQuery query = mock(TaskQuery.class);
        when(taskService.createTaskQuery()).thenReturn(query);
        when(query.taskDefinitionKey(any())).thenReturn(query);
        when(query.active()).thenReturn(query);
        when(query.list()).thenReturn(returns);
        return query;
    }

    private ProcessInstanceQuery stubProcessInstanceQuery(String id, String businessKey) {
        ProcessInstance instance = mock(ProcessInstance.class);
        when(instance.getId()).thenReturn(id);
        when(instance.getBusinessKey()).thenReturn(businessKey);
        ProcessInstanceQuery query = mock(ProcessInstanceQuery.class);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(query);
        when(query.processInstanceIds(any())).thenReturn(query);
        when(query.list()).thenReturn(List.of(instance));
        return query;
    }
}
