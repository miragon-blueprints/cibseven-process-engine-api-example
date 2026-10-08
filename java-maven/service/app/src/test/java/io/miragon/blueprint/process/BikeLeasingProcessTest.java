package io.miragon.blueprint.process;

import static io.miragon.blueprint.process.util.JobExecutionUtils.continueToNextWaitState;
import static io.miragon.blueprint.process.util.ProcessInstanceUtils.findProcessInstance;
import static io.miragon.blueprint.process.util.TimerUtils.fireTimer;
import static org.cibseven.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.cibseven.bpm.engine.test.assertions.bpmn.BpmnAwareTests.init;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.Elements;
import io.miragon.blueprint.adapter.process.CancelBikeOrderProcessApi;
import io.miragon.blueprint.application.port.inbound.ActivateLeasingUseCase;
import io.miragon.blueprint.application.port.inbound.BookCancellationCostsUseCase;
import io.miragon.blueprint.application.port.inbound.CancelContractUseCase;
import io.miragon.blueprint.application.port.inbound.CancelInsurancePolicyUseCase;
import io.miragon.blueprint.application.port.inbound.IssueInsurancePolicyUseCase;
import io.miragon.blueprint.application.port.inbound.OrderBikeUseCase;
import io.miragon.blueprint.application.port.inbound.RejectApplicationUseCase;
import io.miragon.blueprint.application.port.inbound.RequestOrderCancellationUseCase;
import io.miragon.blueprint.application.port.inbound.SendCancellationConfirmationUseCase;
import io.miragon.blueprint.application.port.inbound.SendContractUseCase;
import io.miragon.blueprint.application.port.inbound.SendSignatureReminderUseCase;
import io.miragon.blueprint.application.port.inbound.ValidateApplicationUseCase;
import io.miragon.blueprint.application.port.outbound.LeasingProcess;
import io.miragon.blueprint.domain.bike.BikeId;
import io.miragon.blueprint.domain.bike.OrderId;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import io.miragon.blueprint.domain.leasing.ApplicationInvalidException;
import io.miragon.blueprint.domain.leasing.CustomerName;
import io.miragon.blueprint.domain.leasing.Email;
import io.miragon.blueprint.domain.leasing.LeasingApplication;
import io.miragon.blueprint.domain.leasing.LeasingStatus;
import java.time.LocalDateTime;
import java.util.Map;
import org.cibseven.bpm.engine.ProcessEngine;
import org.cibseven.bpm.engine.RuntimeService;
import org.cibseven.bpm.engine.TaskService;
import org.cibseven.bpm.engine.runtime.ProcessInstance;
import org.cibseven.bpm.engine.task.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Drives the deployed model end-to-end. Unlike the classic-delegate blueprint, the service tasks are
 * external tasks completed by the real, asynchronously polling {@code @ProcessEngineWorker} beans (which
 * call the mocked use cases). Each step therefore drives the process with
 * {@link io.miragon.blueprint.process.util.JobExecutionUtils#continueToNextWaitState(ProcessEngine)},
 * which fires async continuations <em>and</em> waits for the workers to drain the open external tasks, then
 * correlates messages / fires timers / completes the user task through the {@link LeasingProcess} port.
 */
@SpringBootTest
@ActiveProfiles("test")
class BikeLeasingProcessTest {

    @Autowired
    private LeasingProcess process;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private ProcessEngine processEngine;

    @MockitoBean
    private ValidateApplicationUseCase validateApplicationUseCase;

    @MockitoBean
    private RejectApplicationUseCase rejectApplicationUseCase;

    @MockitoBean
    private SendContractUseCase sendContractUseCase;

    @MockitoBean
    private CancelContractUseCase cancelContractUseCase;

    @MockitoBean
    private IssueInsurancePolicyUseCase issueInsurancePolicyUseCase;

    @MockitoBean
    private CancelInsurancePolicyUseCase cancelInsurancePolicyUseCase;

    @MockitoBean
    private SendSignatureReminderUseCase sendSignatureReminderUseCase;

    @MockitoBean
    private SendCancellationConfirmationUseCase sendCancellationConfirmationUseCase;

    @MockitoBean
    private RequestOrderCancellationUseCase requestOrderCancellationUseCase;

    @MockitoBean
    private BookCancellationCostsUseCase bookCancellationCostsUseCase;

    @MockitoBean
    private OrderBikeUseCase orderBikeUseCase;

    @MockitoBean
    private ActivateLeasingUseCase activateLeasingUseCase;

    @BeforeEach
    void setUp() {
        init(processEngine);
        when(orderBikeUseCase.orderBike(any()))
            .thenReturn(new OrderBikeUseCase.Result(new OrderId("ORDER-1"), true));
    }

    @Test
    void happy_path_contract_signed_bike_available_leasing_becomes_active() throws InterruptedException {
        ApplicationId id = submit(35, 3500.0);
        ProcessInstance instance = findProcessInstance(runtimeService, id);

        // validate -> DMN -> conclude-contract sub-process parks on the signature wait state
        continueToNextWaitState(processEngine);

        process.correlateContractSigned(id); // forks into insurance + bike order, joins -> handover wait state
        continueToNextWaitState(processEngine);

        process.correlateHandoverReported(id); // -> withdrawal-period timer
        continueToNextWaitState(processEngine);

        fireTimer(processEngine, Elements.EVENT_WITHDRAWAL_PERIOD_ELAPSED);
        continueToNextWaitState(processEngine);

        assertThat(instance)
            .isEnded()
            .hasPassedInOrder(
                Elements.SERVICE_TASK_VALIDATE_APPLICATION.getValue(),
                Elements.BUSINESS_RULE_TASK_CHECK_CREDIT_RATING.getValue(),
                Elements.SERVICE_TASK_SEND_CONTRACT.getValue(),
                Elements.SERVICE_TASK_ISSUE_INSURANCE_POLICY.getValue(),
                Elements.EVENT_HANDOVER_REPORTED.getValue(),
                Elements.SERVICE_TASK_ACTIVATE_LEASING.getValue(),
                Elements.END_EVENT_LEASING_ACTIVE.getValue())
            .hasNotPassed(
                Elements.END_EVENT_APPLICATION_REJECTED.getValue(),
                Elements.END_EVENT_APPLICATION_CANCELLED.getValue(),
                Elements.END_EVENT_CONTRACT_CANCELLED.getValue());

        verify(sendContractUseCase, times(1)).sendContract(id);
        verify(issueInsurancePolicyUseCase, times(1)).issuePolicy(id);
        verify(activateLeasingUseCase, times(1)).activate(id);
    }

    @Test
    void escalation_contract_not_signed_in_time_is_escalated_and_rejected() throws InterruptedException {
        ApplicationId id = submit(35, 3500.0);
        ProcessInstance instance = findProcessInstance(runtimeService, id);

        continueToNextWaitState(processEngine); // parks on the signature wait state

        fireTimer(processEngine, Elements.EVENT_SIGNATURE_DEADLINE); // deadline -> escalation -> rejection
        continueToNextWaitState(processEngine);

        assertThat(instance)
            .isEnded()
            .hasPassed(
                Elements.EVENT_SIGNATURE_DEADLINE.getValue(),
                Elements.EVENT_CONTRACT_NOT_SIGNED.getValue(),
                Elements.SERVICE_TASK_SEND_REJECTION.getValue(),
                Elements.END_EVENT_APPLICATION_REJECTED.getValue())
            .hasNotPassed(Elements.END_EVENT_LEASING_ACTIVE.getValue());

        verify(rejectApplicationUseCase, times(1)).reject(id);
    }

    @Test
    void not_solvent_the_DMN_routes_the_application_straight_to_rejection() throws InterruptedException {
        // age below 18 cannot sign a leasing contract, so the DMN returns solvent = false
        ApplicationId id = submit(15, 3500.0);

        continueToNextWaitState(processEngine); // validate -> DMN -> not solvent -> rejection -> end

        verify(rejectApplicationUseCase, times(1)).reject(id);
        verify(sendContractUseCase, never()).sendContract(any());
    }

    @Test
    void invalid_application_the_applicationInvalid_BPMN_error_routes_it_straight_to_rejection()
        throws InterruptedException {
        // The use case rejects the application; the worker translates that into the checked
        // BpmnErrorOccurred, which the boundary error event on the validate task catches.
        doAnswer(invocation -> {
            throw new ApplicationInvalidException(
                invocation.getArgument(0), "monthly net income must be greater than zero");
        }).when(validateApplicationUseCase).validate(any());

        ApplicationId id = submit(35, 0.0);
        ProcessInstance instance = findProcessInstance(runtimeService, id);

        continueToNextWaitState(processEngine); // validate -> applicationInvalid -> rejection -> end

        assertThat(instance)
            .isEnded()
            .hasPassedInOrder(
                Elements.SERVICE_TASK_VALIDATE_APPLICATION.getValue(),
                Elements.EVENT_APPLICATION_INVALID.getValue(),
                Elements.SERVICE_TASK_SEND_REJECTION.getValue(),
                Elements.END_EVENT_APPLICATION_REJECTED.getValue())
            .hasNotPassed(
                Elements.BUSINESS_RULE_TASK_CHECK_CREDIT_RATING.getValue(),
                Elements.SERVICE_TASK_SEND_CONTRACT.getValue(),
                Elements.END_EVENT_LEASING_ACTIVE.getValue());

        verify(validateApplicationUseCase, times(1)).validate(id);
        verify(rejectApplicationUseCase, times(1)).reject(id);
        verify(sendContractUseCase, never()).sendContract(any());
    }

    @Test
    void abort_withdrawing_the_application_compensates_the_completed_steps() throws InterruptedException {
        when(requestOrderCancellationUseCase.requestCancellation(any())).thenReturn(true);

        ApplicationId id = submit(35, 3500.0);
        ProcessInstance instance = findProcessInstance(runtimeService, id);

        // drive up to the handover wait state (contract signed, bike ordered, insured)
        continueToNextWaitState(processEngine);
        process.correlateContractSigned(id);
        continueToNextWaitState(processEngine);

        // Withdrawing triggers compensation; its handlers run in an engine-defined order, so drive the
        // continuations generically until the cancelBikeOrder sub-process parks on its user task.
        process.correlateApplicationWithdrawn(id);
        continueToNextWaitState(processEngine);

        Task task =
            taskService
                .createTaskQuery()
                .taskDefinitionKey(CancelBikeOrderProcessApi.Elements.USER_TASK_CLARIFY_RETURN.getValue())
                .singleResult();
        taskService.complete(task.getId(), Map.of("returnClarified", true));
        continueToNextWaitState(processEngine);

        assertThat(instance)
            .isEnded()
            .hasPassed(
                Elements.SERVICE_TASK_CANCEL_CONTRACT.getValue(),
                Elements.SERVICE_TASK_CANCEL_POLICY.getValue(),
                Elements.CALL_ACTIVITY_CANCEL_BIKE_ORDER.getValue(),
                Elements.SERVICE_TASK_SEND_CANCELLATION_CONFIRMATION.getValue(),
                Elements.END_EVENT_APPLICATION_CANCELLED.getValue())
            .hasNotPassed(Elements.END_EVENT_LEASING_ACTIVE.getValue());

        // External-task delivery is at-least-once: while compensation runs several handlers in
        // parallel, an async continuation firing on the test thread can force an optimistic-locking
        // retry of a handler that already ran, so a worker's use case may be invoked more than once
        // (workers are expected to be idempotent). The process still ends correctly, as asserted above.
        verify(cancelContractUseCase, atLeastOnce()).cancelContract(id);
        verify(cancelInsurancePolicyUseCase, atLeastOnce()).cancelPolicy(id);
        verify(sendCancellationConfirmationUseCase, atLeastOnce()).sendCancellationConfirmation(id);
    }

    @Test
    void bike_unavailable_clarifying_an_alternative_re_orders_and_leasing_becomes_active()
        throws InterruptedException {
        // the first order finds the requested bike unavailable, the re-order after the alternative succeeds
        when(orderBikeUseCase.orderBike(any()))
            .thenReturn(
                new OrderBikeUseCase.Result(null, false),
                new OrderBikeUseCase.Result(new OrderId("ORDER-2"), true));

        ApplicationId id = submit(35, 3500.0);
        ProcessInstance instance = findProcessInstance(runtimeService, id);

        continueToNextWaitState(processEngine); // parks on the signature wait state
        process.correlateContractSigned(id);
        continueToNextWaitState(processEngine); // fork -> order finds bike unavailable -> parks on clarify-alternative

        // the alternative is clarified from the outside — the "external" completion of the user task
        process.completeAlternativeClarification(id, true, new BikeId("BIKE-ALT"));
        continueToNextWaitState(processEngine); // re-order succeeds -> parallel join -> handover wait state

        process.correlateHandoverReported(id);
        continueToNextWaitState(processEngine);
        fireTimer(processEngine, Elements.EVENT_WITHDRAWAL_PERIOD_ELAPSED);
        continueToNextWaitState(processEngine);

        assertThat(instance)
            .isEnded()
            .hasPassed(
                Elements.USER_TASK_CLARIFY_ALTERNATIVE.getValue(),
                Elements.SERVICE_TASK_ORDER_BIKE.getValue(),
                Elements.END_EVENT_LEASING_ACTIVE.getValue())
            .hasNotPassed(
                Elements.END_EVENT_CONTRACT_CANCELLED.getValue(),
                Elements.END_EVENT_APPLICATION_REJECTED.getValue());

        verify(orderBikeUseCase, times(2)).orderBike(id);
    }

    private ApplicationId submit(int age, double income) {
        LeasingApplication application =
            new LeasingApplication(
                ApplicationId.newId(),
                new CustomerName("Test Customer"),
                new Email("test@example.com"),
                age,
                income,
                new BikeId("BIKE-TEST"),
                LeasingStatus.RECEIVED,
                LocalDateTime.now(),
                null,
                null);
        process.submitRequest(application);
        return application.id();
    }
}
