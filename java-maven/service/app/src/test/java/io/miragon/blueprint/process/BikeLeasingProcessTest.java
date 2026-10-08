package io.miragon.blueprint.process;

import static io.miragon.blueprint.process.util.JobExecutionUtils.continueToNextWaitState;
import static io.miragon.blueprint.process.util.ProcessInstanceUtils.findProcessInstance;
import static io.miragon.blueprint.process.util.TimerUtils.fireTimer;
import static org.cibseven.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.cibseven.bpm.engine.test.assertions.bpmn.BpmnAwareTests.init;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.FlowNodes;
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
import io.miragon.blueprint.application.port.outbound.LeasingProcess;
import io.miragon.blueprint.domain.bike.BikeId;
import io.miragon.blueprint.domain.bike.BikeUnavailableException;
import io.miragon.blueprint.domain.bike.OrderId;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import io.miragon.blueprint.domain.leasing.CustomerName;
import io.miragon.blueprint.domain.leasing.Email;
import io.miragon.blueprint.domain.leasing.LeasingApplication;
import io.miragon.blueprint.domain.leasing.LeasingStatus;
import io.miragon.bpmn.runtime.path.PathWalk;
import java.time.LocalDateTime;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.cibseven.bpm.engine.HistoryService;
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
    private HistoryService historyService;

    @Autowired
    private ProcessEngine processEngine;

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
        when(orderBikeUseCase.orderBike(any())).thenReturn(new OrderId("ORDER-1"));
    }

    @Test
    void happy_path_contract_signed_bike_available_leasing_becomes_active() throws InterruptedException {
        ApplicationId id = submit(35, 3500.0);
        ProcessInstance instance = findProcessInstance(runtimeService, id);

        // DMN -> conclude-contract sub-process parks on the signature wait state
        continueToNextWaitState(processEngine);

        process.correlateContractSigned(id); // forks into insurance + bike order, joins -> handover wait state
        continueToNextWaitState(processEngine);

        process.correlateHandoverReported(id); // -> withdrawal-period timer
        continueToNextWaitState(processEngine);

        fireTimer(processEngine, FlowNodes.EventWithdrawalPeriodElapsed.INSTANCE);
        continueToNextWaitState(processEngine);

        assertThat(instance)
            .isEnded()
            .hasPassedInOrder(
                pathUntilContractSigned()
                    .then(next -> next.gatewayFork())
                    .then(next -> next.serviceTaskIssueInsurancePolicy())
                    .then(next -> next.gatewayJoin())
                    .then(next -> next.eventHandoverReported())
                    .then(next -> next.eventWithdrawalPeriodElapsed())
                    .then(next -> next.serviceTaskActivateLeasing())
                    .end(next -> next.endEventLeasingActive())
                    .getIds())
            .hasPassedInOrder(
                PathWalk.from(FlowNodes.GatewayFork.INSTANCE)
                    .then(next -> next.gatewayBikeSourceJoin())
                    .then(next -> next.serviceTaskOrderBike())
                    .then(next -> next.gatewayJoin())
                    .getIds())
            .hasNotPassed(
                FlowNodes.EventBikeUnavailable.ELEMENT_ID,
                FlowNodes.EndEventApplicationRejected.ELEMENT_ID,
                FlowNodes.EndEventApplicationCancelled.ELEMENT_ID,
                FlowNodes.EndEventContractCancelled.ELEMENT_ID);

        verify(sendContractUseCase, times(1)).sendContract(id);
        verify(issueInsurancePolicyUseCase, times(1)).issuePolicy(id);
        verify(activateLeasingUseCase, times(1)).activate(id);
    }

    @Test
    void escalation_contract_not_signed_in_time_is_escalated_and_rejected() throws InterruptedException {
        ApplicationId id = submit(35, 3500.0);
        ProcessInstance instance = findProcessInstance(runtimeService, id);

        continueToNextWaitState(processEngine); // parks on the signature wait state

        fireTimer(processEngine, FlowNodes.EventSignatureDeadline.INSTANCE); // deadline -> escalation -> rejection
        continueToNextWaitState(processEngine);

        assertThat(instance)
            .isEnded()
            .hasPassedInOrder(
                pathUntilCreditRatingChecked()
                    .onto(next -> next.subProcessConcludeContract())
                    .inside(FlowNodes.SubProcessConcludeContract.INSTANCE, start ->
                        pathUntilSignatureAwaited(start)
                            .then(next -> next.eventSignatureDeadline())
                            .end(next -> next.endEventNotSigned()))
                    .interruptedBy(
                        FlowNodes.SubProcessConcludeContract.INSTANCE,
                        next -> next.eventContractNotSigned())
                    .then(next -> next.gatewayRejectionJoin())
                    .then(next -> next.serviceTaskSendRejection())
                    .end(next -> next.endEventApplicationRejected())
                    .getIds())
            .hasNotPassed(FlowNodes.EndEventLeasingActive.ELEMENT_ID);

        verify(rejectApplicationUseCase, times(1)).reject(id);
    }

    @Test
    void not_solvent_the_DMN_routes_the_application_straight_to_rejection() throws InterruptedException {
        // age below 18 cannot sign a leasing contract, so the DMN returns solvent = false
        ApplicationId id = submit(15, 3500.0);
        ProcessInstance instance = findProcessInstance(runtimeService, id);

        continueToNextWaitState(processEngine); // DMN -> not solvent -> rejection -> end

        assertThat(instance)
            .isEnded()
            .hasPassedInOrder(
                pathUntilCreditRatingChecked()
                    .then(next -> next.gatewayRejectionJoin())
                    .then(next -> next.serviceTaskSendRejection())
                    .end(next -> next.endEventApplicationRejected())
                    .getIds())
            .hasNotPassed(FlowNodes.SubProcessConcludeContract.ELEMENT_ID);

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
                .taskDefinitionKey(CancelBikeOrderProcessApi.FlowNodes.UserTaskClarifyReturn.ELEMENT_ID)
                .singleResult();
        taskService.complete(task.getId(), Map.of("returnClarified", true));
        continueToNextWaitState(processEngine);

        assertThat(instance)
            .isEnded()
            .hasPassed(
                PathWalk.from(FlowNodes.StartEventApplicationWithdrawn.INSTANCE)
                    .then(next -> next.eventReverseApplication())
                    .throwingCompensation(
                        FlowNodes.EventCompensateContract.INSTANCE,
                        next -> next.serviceTaskCancelContract())
                    .throwingCompensation(
                        FlowNodes.EventCompensateInsurance.INSTANCE,
                        next -> next.serviceTaskCancelPolicy())
                    .throwingCompensation(
                        FlowNodes.EventCompensateOrder.INSTANCE,
                        next -> next.callActivityCancelBikeOrder())
                    .then(next -> next.serviceTaskSendCancellationConfirmation())
                    .end(next -> next.endEventApplicationCancelled())
                    .getDistinctIds())
            .hasNotPassed(FlowNodes.EndEventLeasingActive.ELEMENT_ID);

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
        ApplicationId id = submitUntilBikeUnavailable();
        ProcessInstance instance = findProcessInstance(runtimeService, id);

        doReturn(new OrderId("ORDER-2")).when(orderBikeUseCase).orderBike(any()); // the alternative is in stock
        // the alternative is clarified from the outside — the "external" completion of the user task
        process.completeAlternativeClarification(id, true, new BikeId("BIKE-ALT"));
        continueToNextWaitState(processEngine); // re-order succeeds -> parallel join -> handover wait state

        process.correlateHandoverReported(id);
        continueToNextWaitState(processEngine);
        fireTimer(processEngine, FlowNodes.EventWithdrawalPeriodElapsed.INSTANCE);
        continueToNextWaitState(processEngine);

        assertThat(instance)
            .isEnded()
            .hasPassedInOrder(
                PathWalk.from(FlowNodes.GatewayFork.INSTANCE)
                    .then(next -> next.gatewayBikeSourceJoin())
                    .then(next -> next.serviceTaskOrderBike())
                    .interruptedBy(
                        FlowNodes.ServiceTaskOrderBike.INSTANCE,
                        next -> next.eventBikeUnavailable())
                    .then(next -> next.userTaskClarifyAlternative())
                    .then(next -> next.gatewayAlternativeFound())
                    .then(next -> next.gatewayBikeSourceJoin())
                    .then(next -> next.serviceTaskOrderBike())
                    .then(next -> next.gatewayJoin())
                    .then(next -> next.eventHandoverReported())
                    .then(next -> next.eventWithdrawalPeriodElapsed())
                    .then(next -> next.serviceTaskActivateLeasing())
                    .end(next -> next.endEventLeasingActive())
                    .getIds())
            .hasNotPassed(
                FlowNodes.EndEventContractCancelled.ELEMENT_ID,
                FlowNodes.EndEventApplicationRejected.ELEMENT_ID);

        verify(orderBikeUseCase, times(2)).orderBike(id);
    }

    @Test
    void bike_unavailable_declining_the_alternative_reverses_contract_and_policy_without_cancelling_an_order()
        throws InterruptedException {
        ApplicationId id = submitUntilBikeUnavailable();
        ProcessInstance instance = findProcessInstance(runtimeService, id);

        process.completeAlternativeClarification(id, false, null);
        continueToNextWaitState(processEngine); // -> compensation -> sendCancellationConfirmation -> end cancelled

        assertThat(instance)
            .isEnded()
            .hasPassed(
                PathWalk.from(FlowNodes.GatewayAlternativeFound.INSTANCE)
                    .then(next -> next.eventTriggerReversal())
                    .throwingCompensation(
                        FlowNodes.EventCompensateContract.INSTANCE,
                        next -> next.serviceTaskCancelContract())
                    .throwingCompensation(
                        FlowNodes.EventCompensateInsurance.INSTANCE,
                        next -> next.serviceTaskCancelPolicy())
                    .then(next -> next.serviceTaskConfirmContractCancellation())
                    .end(next -> next.endEventContractCancelled())
                    .getDistinctIds())
            .hasNotPassed(
                FlowNodes.CallActivityCancelBikeOrder.ELEMENT_ID,
                FlowNodes.EndEventLeasingActive.ELEMENT_ID);

        verify(sendCancellationConfirmationUseCase, atLeastOnce()).sendCancellationConfirmation(id);
        verify(requestOrderCancellationUseCase, never()).requestCancellation(any());
    }

    @Test
    void abort_while_clarifying_an_alternative_compensates_contract_and_policy_without_cancelling_an_order()
        throws InterruptedException {
        ApplicationId id = submitUntilBikeUnavailable();
        ProcessInstance instance = findProcessInstance(runtimeService, id);

        process.correlateApplicationWithdrawn(id);
        continueToNextWaitState(processEngine); // -> compensation -> sendCancellationConfirmation -> end cancelled

        assertThat(instance)
            .isEnded()
            .hasPassed(
                PathWalk.from(FlowNodes.StartEventApplicationWithdrawn.INSTANCE)
                    .then(next -> next.eventReverseApplication())
                    .throwingCompensation(
                        FlowNodes.EventCompensateContract.INSTANCE,
                        next -> next.serviceTaskCancelContract())
                    .throwingCompensation(
                        FlowNodes.EventCompensateInsurance.INSTANCE,
                        next -> next.serviceTaskCancelPolicy())
                    .then(next -> next.serviceTaskSendCancellationConfirmation())
                    .end(next -> next.endEventApplicationCancelled())
                    .getDistinctIds())
            .hasNotPassed(FlowNodes.CallActivityCancelBikeOrder.ELEMENT_ID);

        verify(requestOrderCancellationUseCase, never()).requestCancellation(any());
    }

    @Test
    void abort_after_an_accepted_alternative_cancels_the_one_placed_order_exactly_once()
        throws InterruptedException {
        when(requestOrderCancellationUseCase.requestCancellation(any())).thenReturn(true);

        ApplicationId id = submitUntilBikeUnavailable();
        ProcessInstance instance = findProcessInstance(runtimeService, id);

        doReturn(new OrderId("ORDER-2")).when(orderBikeUseCase).orderBike(any()); // the alternative is in stock
        process.completeAlternativeClarification(id, true, new BikeId("BIKE-ALT"));
        continueToNextWaitState(processEngine); // re-order succeeds -> parallel join -> handover wait state

        process.correlateApplicationWithdrawn(id);
        continueToNextWaitState(processEngine);

        Task task =
            taskService
                .createTaskQuery()
                .taskDefinitionKey(CancelBikeOrderProcessApi.FlowNodes.UserTaskClarifyReturn.ELEMENT_ID)
                .singleResult();
        taskService.complete(task.getId(), Map.of("returnClarified", true));
        continueToNextWaitState(processEngine);

        assertThat(instance).isEnded().hasPassed(FlowNodes.EndEventApplicationCancelled.ELEMENT_ID);
        long orderCancellations =
            historyService
                .createHistoricProcessInstanceQuery()
                .processDefinitionKey(CancelBikeOrderProcessApi.PROCESS_ID.getValue())
                .superProcessInstanceId(instance.getId())
                .count();
        Assertions.assertThat(orderCancellations).isEqualTo(1);
    }

    /** Drives a signed, insured application to the clarify-alternative task: the dealer has no bike. */
    private ApplicationId submitUntilBikeUnavailable() throws InterruptedException {
        when(orderBikeUseCase.orderBike(any())).thenThrow(new BikeUnavailableException(new BikeId("BIKE-TEST")));

        ApplicationId id = submit(35, 3500.0);
        continueToNextWaitState(processEngine); // parks on the signature wait state
        process.correlateContractSigned(id);
        continueToNextWaitState(processEngine); // fork -> order raises bikeUnavailable -> parks on clarify-alternative
        return id;
    }

    private PathWalk<FlowNodes.GatewayIsSolvent, FlowNodes.GatewayIsSolvent.Next> pathUntilCreditRatingChecked() {
        return PathWalk.from(FlowNodes.StartEventLeasingRequestReceived.INSTANCE)
            .then(next -> next.businessRuleTaskCheckCreditRating())
            .then(next -> next.gatewayIsSolvent());
    }

    private PathWalk<FlowNodes.GatewayAwaitSignature, FlowNodes.GatewayAwaitSignature.Next> pathUntilSignatureAwaited(
        FlowNodes.SubProcessConcludeContract.Start start) {
        return PathWalk.from(start.startEventCustomerEligible())
            .then(next -> next.serviceTaskSendContract())
            .then(next -> next.gatewayAwaitSignature());
    }

    private PathWalk<FlowNodes.SubProcessConcludeContract, FlowNodes.SubProcessConcludeContract.Next>
        pathUntilContractSigned() {
        return pathUntilCreditRatingChecked()
            .onto(next -> next.subProcessConcludeContract())
            .inside(FlowNodes.SubProcessConcludeContract.INSTANCE, start ->
                pathUntilSignatureAwaited(start)
                    .then(next -> next.eventContractSigned())
                    .end(next -> next.endEventContractValid()));
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
