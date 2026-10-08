package io.miragon.blueprint.adapter.outbound.cibseven;

import static io.miragon.blueprint.domain.leasing.TestObjectBuilder.testLeasingApplication;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dev.bpmcrafters.processengineapi.CommonRestrictions;
import dev.bpmcrafters.processengineapi.Empty;
import dev.bpmcrafters.processengineapi.correlation.CorrelateMessageCmd;
import dev.bpmcrafters.processengineapi.correlation.CorrelationApi;
import dev.bpmcrafters.processengineapi.process.ProcessInformation;
import dev.bpmcrafters.processengineapi.process.StartProcessApi;
import dev.bpmcrafters.processengineapi.process.StartProcessByMessageCmd;
import dev.bpmcrafters.processengineapi.task.CompleteTaskCmd;
import dev.bpmcrafters.processengineapi.task.TaskInformation;
import dev.bpmcrafters.processengineapi.task.UserTaskCompletionApi;
import dev.bpmcrafters.processengineapi.task.support.UserTaskSupport;
import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.Elements;
import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.Messages;
import io.miragon.blueprint.domain.bike.BikeId;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import io.miragon.blueprint.domain.leasing.LeasingApplication;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.cibseven.bpm.engine.MismatchingMessageCorrelationException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LeasingProcessAdapterTest {

    private final StartProcessApi startProcessApi = mock(StartProcessApi.class);
    private final CorrelationApi correlationApi = mock(CorrelationApi.class);
    private final UserTaskSupport userTaskSupport = mock(UserTaskSupport.class);
    private final UserTaskCompletionApi userTaskCompletionApi = mock(UserTaskCompletionApi.class);
    private final LeasingProcessAdapter underTest = new LeasingProcessAdapter(
            startProcessApi,
            correlationApi,
            userTaskSupport,
            userTaskCompletionApi);

    private final ApplicationId id = new ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));

    @Test
    void submitRequest_starts_the_process_by_message_with_the_application_variables_and_keys() {

        // given: a leasing application and a captured start command
        LeasingApplication application = testLeasingApplication().id(id).build();
        when(startProcessApi.startProcess(any()))
                .thenReturn(CompletableFuture.completedFuture(mock(ProcessInformation.class)));

        // when: the request is submitted
        underTest.submitRequest(application);

        // then: the leasing-request message starts the process with the DMN inputs, bike, business + correlation key
        ArgumentCaptor<StartProcessByMessageCmd> cmd = ArgumentCaptor.forClass(StartProcessByMessageCmd.class);
        verify(startProcessApi).startProcess(cmd.capture());
        assertThat(cmd.getValue().getMessageName()).isEqualTo(Messages.MIRAVELO_LEASING_REQUEST_RECEIVED.getValue());
        Map<String, Object> payload = cmd.getValue().get();
        assertThat(payload.get("applicationId")).isEqualTo(id.value().toString());
        assertThat(payload.get("age")).isEqualTo(35);
        assertThat(payload.get("monthlyNetIncome")).isEqualTo(3500.0);
        assertThat(payload.get("bikeId")).isEqualTo("BIKE-900");
        assertThat(payload.get(CommonRestrictions.BUSINESS_KEY)).isEqualTo(id.value().toString());
        assertThat(payload.get(CommonRestrictions.CORRELATION_KEY)).isEqualTo(id.value().toString());
    }

    @Test
    void completeAlternativeClarification_resolves_the_task_by_application_id_and_completes_it() {

        // given: an open clarify-alternative task delivered to the user-task pool for this application
        TaskInformation task = clarifyAlternativeTask("task-1");
        when(userTaskSupport.getAllTasks()).thenReturn(List.of(task));
        when(userTaskSupport.getPayload("task-1")).thenReturn(Map.of("applicationId", id.value().toString()));
        when(userTaskCompletionApi.completeTask(any())).thenReturn(CompletableFuture.completedFuture(Empty.INSTANCE));

        // when: an alternative bike is selected from the outside
        underTest.completeAlternativeClarification(id, true, new BikeId("BIKE-42"));

        // then: the resolved task is completed with the decision and the chosen bike
        ArgumentCaptor<CompleteTaskCmd> cmd = ArgumentCaptor.forClass(CompleteTaskCmd.class);
        verify(userTaskCompletionApi).completeTask(cmd.capture());
        assertThat(cmd.getValue().getTaskId()).isEqualTo("task-1");
        Map<String, Object> payload = cmd.getValue().get();
        assertThat(payload.get("alternativeFound")).isEqualTo(true);
        assertThat(payload.get("bikeId")).isEqualTo("BIKE-42");
    }

    @Test
    void completeAlternativeClarification_skips_a_pooled_task_whose_payload_cannot_be_read() {

        // given: a stale clarify-alternative task whose payload lookup fails, next to the one of this application
        TaskInformation stale = clarifyAlternativeTask("task-stale");
        TaskInformation task = clarifyAlternativeTask("task-1");
        when(userTaskSupport.getAllTasks()).thenReturn(List.of(stale, task));
        when(userTaskSupport.getPayload("task-stale")).thenThrow(new IllegalArgumentException("unknown task"));
        when(userTaskSupport.getPayload("task-1")).thenReturn(Map.of("applicationId", id.value().toString()));
        when(userTaskCompletionApi.completeTask(any())).thenReturn(CompletableFuture.completedFuture(Empty.INSTANCE));

        // when: the clarification ends without an alternative
        underTest.completeAlternativeClarification(id, false, null);

        // then: the failing task counts as no match; the matching task is completed with the decision only
        ArgumentCaptor<CompleteTaskCmd> cmd = ArgumentCaptor.forClass(CompleteTaskCmd.class);
        verify(userTaskCompletionApi).completeTask(cmd.capture());
        assertThat(cmd.getValue().getTaskId()).isEqualTo("task-1");
        assertThat(cmd.getValue().get()).containsExactlyEntriesOf(Map.of("alternativeFound", false));
    }

    @Test
    void completeAlternativeClarification_ignores_pooled_tasks_of_other_activities_and_applications() {

        // given: a task of another activity for this application and a clarify-alternative task of another
        // application, both pooled before the clarify-alternative task of this application
        TaskInformation otherActivity = new TaskInformation(
                "task-other-activity",
                Map.of(CommonRestrictions.ACTIVITY_ID, "userTask_somethingElse"));
        TaskInformation otherApplication = clarifyAlternativeTask("task-other-application");
        TaskInformation task = clarifyAlternativeTask("task-1");
        when(userTaskSupport.getAllTasks()).thenReturn(List.of(otherActivity, otherApplication, task));
        when(userTaskSupport.getPayload("task-other-activity"))
                .thenReturn(Map.of("applicationId", id.value().toString()));
        when(userTaskSupport.getPayload("task-other-application"))
                .thenReturn(Map.of("applicationId", UUID.randomUUID().toString()));
        when(userTaskSupport.getPayload("task-1")).thenReturn(Map.of("applicationId", id.value().toString()));
        when(userTaskCompletionApi.completeTask(any())).thenReturn(CompletableFuture.completedFuture(Empty.INSTANCE));

        // when: an alternative bike is selected from the outside
        underTest.completeAlternativeClarification(id, true, new BikeId("BIKE-42"));

        // then: only the clarify-alternative task of this application is completed
        ArgumentCaptor<CompleteTaskCmd> cmd = ArgumentCaptor.forClass(CompleteTaskCmd.class);
        verify(userTaskCompletionApi).completeTask(cmd.capture());
        assertThat(cmd.getValue().getTaskId()).isEqualTo("task-1");
    }

    @Test
    void completeAlternativeClarification_fails_and_keeps_the_interrupt_when_the_task_wait_is_interrupted() {

        // given: no clarify-alternative task in the pool yet and an interrupted caller
        when(userTaskSupport.getAllTasks()).thenReturn(List.of());
        Thread.currentThread().interrupt();
        try {
            // when / then: the wait aborts at once with an unchecked exception wrapping the interrupt
            assertThatThrownBy(() -> underTest.completeAlternativeClarification(id, true, null))
                    .isExactlyInstanceOf(RuntimeException.class)
                    .hasMessage("Interrupted while waiting for the clarify-alternative task")
                    .hasCauseInstanceOf(InterruptedException.class);
            // and: the interrupt flag is restored for the caller
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            // clear the flag so it cannot leak into other tests
            Thread.interrupted();
        }
        verifyNoInteractions(userTaskCompletionApi);
    }

    @Test
    void correlateContractSigned_correlates_the_message_by_the_global_correlation_key() {
        assertCorrelation(Messages.MIRAVELO_CONTRACT_SIGNED.getValue(), () -> underTest.correlateContractSigned(id));
    }

    @Test
    void correlateHandoverReported_correlates_the_message_by_the_global_correlation_key() {
        assertCorrelation(
                Messages.MIRAVELO_HANDOVER_REPORTED.getValue(),
                () -> underTest.correlateHandoverReported(id));
    }

    @Test
    void correlateApplicationWithdrawn_correlates_the_message_by_the_global_correlation_key() {
        assertCorrelation(
                Messages.MIRAVELO_APPLICATION_WITHDRAWN.getValue(),
                () -> underTest.correlateApplicationWithdrawn(id));
    }

    @Test
    void correlation_rethrows_an_unchecked_engine_failure_unwrapped() {

        // given: the engine rejects the correlation, e.g. because the token already left the wait state
        MismatchingMessageCorrelationException failure =
                new MismatchingMessageCorrelationException("no execution waits for the message");
        when(correlationApi.correlateMessage(any())).thenReturn(CompletableFuture.failedFuture(failure));

        // when / then: the very engine exception surfaces (the REST advice maps it to 409), not the async wrapper
        assertThatThrownBy(() -> underTest.correlateContractSigned(id)).isSameAs(failure);
    }

    @Test
    void correlation_rethrows_an_error_unwrapped() {

        // given: the correlation fails with an Error
        Error failure = new Error("engine failure");
        when(correlationApi.correlateMessage(any())).thenReturn(CompletableFuture.failedFuture(failure));

        // when / then: the Error itself surfaces, not the async wrapper
        assertThatThrownBy(() -> underTest.correlateHandoverReported(id)).isSameAs(failure);
    }

    @Test
    void correlation_rethrows_the_completion_exception_for_a_checked_cause() {

        // given: the correlation fails with a checked exception, which cannot be rethrown unwrapped
        Exception failure = new Exception("checked failure");
        when(correlationApi.correlateMessage(any())).thenReturn(CompletableFuture.failedFuture(failure));

        // when / then: the CompletionException carrying the checked cause surfaces instead
        assertThatThrownBy(() -> underTest.correlateApplicationWithdrawn(id))
                .isInstanceOf(CompletionException.class)
                .cause().isSameAs(failure);
    }

    private void assertCorrelation(String expectedMessage, Runnable action) {

        // given: a captured correlate command
        when(correlationApi.correlateMessage(any())).thenReturn(CompletableFuture.completedFuture(Empty.INSTANCE));

        // when: the correlation is triggered
        action.run();

        // then: the expected message correlates to the instance whose global correlationKey equals the id
        ArgumentCaptor<CorrelateMessageCmd> cmd = ArgumentCaptor.forClass(CorrelateMessageCmd.class);
        verify(correlationApi).correlateMessage(cmd.capture());
        assertThat(cmd.getValue().getMessageName()).isEqualTo(expectedMessage);
        assertThat(cmd.getValue().getCorrelation().get().getCorrelationKey()).isEqualTo(id.value().toString());
        assertThat(cmd.getValue().getRestrictions().get("useGlobalCorrelationKey")).isEqualTo("true");
    }

    private static TaskInformation clarifyAlternativeTask(String taskId) {
        return new TaskInformation(
                taskId,
                Map.of(CommonRestrictions.ACTIVITY_ID, Elements.USER_TASK_CLARIFY_ALTERNATIVE.getValue()));
    }
}
