package io.miragon.blueprint.process

import com.ninjasquad.springmockk.MockkBean
import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.FlowNodes
import io.miragon.blueprint.adapter.process.CancelBikeOrderProcessApi
import io.miragon.blueprint.application.port.inbound.ActivateLeasingUseCase
import io.miragon.blueprint.application.port.inbound.BookCancellationCostsUseCase
import io.miragon.blueprint.application.port.inbound.CancelContractUseCase
import io.miragon.blueprint.application.port.inbound.CancelInsurancePolicyUseCase
import io.miragon.blueprint.application.port.inbound.IssueInsurancePolicyUseCase
import io.miragon.blueprint.application.port.inbound.OrderBikeUseCase
import io.miragon.blueprint.application.port.inbound.RejectApplicationUseCase
import io.miragon.blueprint.application.port.inbound.RequestOrderCancellationUseCase
import io.miragon.blueprint.application.port.inbound.SendCancellationConfirmationUseCase
import io.miragon.blueprint.application.port.inbound.SendContractUseCase
import io.miragon.blueprint.application.port.inbound.SendSignatureReminderUseCase
import io.miragon.blueprint.application.port.outbound.LeasingProcess
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.bike.BikeUnavailableException
import io.miragon.blueprint.domain.leasing.CustomerName
import io.miragon.blueprint.domain.leasing.Email
import io.miragon.blueprint.domain.leasing.LeasingApplication
import io.miragon.blueprint.domain.leasing.LeasingStatus
import io.miragon.blueprint.domain.bike.OrderId
import io.miragon.blueprint.process.util.continueToNextWaitState
import io.miragon.blueprint.process.util.findProcessInstance
import io.miragon.blueprint.process.util.fireTimer
import io.miragon.blueprint.process.util.hasPassed
import io.miragon.blueprint.process.util.hasPassedInOrder
import io.miragon.bpmn.runtime.path.ProcessPath
import io.miragon.bpmn.runtime.path.enter
import io.miragon.bpmn.runtime.path.inside
import io.miragon.bpmn.runtime.path.interruptedBy
import io.miragon.bpmn.runtime.path.onto
import io.miragon.bpmn.runtime.path.then
import io.miragon.bpmn.runtime.path.throwingCompensation
import io.mockk.every
import io.mockk.verify
import org.assertj.core.api.Assertions
import org.cibseven.bpm.engine.HistoryService
import org.cibseven.bpm.engine.ProcessEngine
import org.cibseven.bpm.engine.RuntimeService
import org.cibseven.bpm.engine.TaskService
import org.cibseven.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat
import org.cibseven.bpm.engine.test.assertions.bpmn.BpmnAwareTests.init
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

/**
 * Drives the deployed model end-to-end. Unlike the classic-delegate blueprint, the service tasks are
 * external tasks completed by the real, asynchronously polling `@ProcessEngineWorker` beans (which
 * call the mocked use cases). Each step therefore drives the process with [continueToNextWaitState],
 * which fires async continuations *and* waits for the workers to drain the open external tasks, then
 * correlates messages / fires timers / completes the user task through the [LeasingProcess] port.
 */
@SpringBootTest
@ActiveProfiles("test")
class BikeLeasingProcessTest {

    @Autowired
    private lateinit var process: LeasingProcess

    @Autowired
    private lateinit var runtimeService: RuntimeService

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var historyService: HistoryService

    @Autowired
    private lateinit var processEngine: ProcessEngine

    @MockkBean(relaxed = true)
    private lateinit var rejectApplicationUseCase: RejectApplicationUseCase

    @MockkBean(relaxed = true)
    private lateinit var sendContractUseCase: SendContractUseCase

    @MockkBean(relaxed = true)
    private lateinit var cancelContractUseCase: CancelContractUseCase

    @MockkBean(relaxed = true)
    private lateinit var issueInsurancePolicyUseCase: IssueInsurancePolicyUseCase

    @MockkBean(relaxed = true)
    private lateinit var cancelInsurancePolicyUseCase: CancelInsurancePolicyUseCase

    @MockkBean(relaxed = true)
    private lateinit var sendSignatureReminderUseCase: SendSignatureReminderUseCase

    @MockkBean(relaxed = true)
    private lateinit var sendCancellationConfirmationUseCase: SendCancellationConfirmationUseCase

    @MockkBean(relaxed = true)
    private lateinit var requestOrderCancellationUseCase: RequestOrderCancellationUseCase

    @MockkBean(relaxed = true)
    private lateinit var bookCancellationCostsUseCase: BookCancellationCostsUseCase

    @MockkBean(relaxed = true)
    private lateinit var orderBikeUseCase: OrderBikeUseCase

    @MockkBean(relaxed = true)
    private lateinit var activateLeasingUseCase: ActivateLeasingUseCase

    @BeforeEach
    fun setUp() {
        init(processEngine)
        every { orderBikeUseCase.orderBike(any()) } returns OrderId("ORDER-1")
    }

    @Test
    fun `happy path - contract signed, bike available, leasing becomes active`() {
        val id = submit(age = 35, income = 3500.0)
        val instance = runtimeService.findProcessInstance(id)

        // DMN -> conclude-contract sub-process parks on the signature wait state
        processEngine.continueToNextWaitState()

        process.correlateContractSigned(id) // forks into insurance + bike order, joins -> handover wait state
        processEngine.continueToNextWaitState()

        process.correlateHandoverReported(id) // -> withdrawal-period timer
        processEngine.continueToNextWaitState()

        processEngine.fireTimer(FlowNodes.EventWithdrawalPeriodElapsed)
        processEngine.continueToNextWaitState()

        assertThat(instance)
            .isEnded
            .hasPassedInOrder(
                pathUntilContractSigned()
                    .then { it.gatewayFork }
                    .then { it.serviceTaskIssueInsurancePolicy }
                    .then { it.gatewayJoin }
                    .then { it.eventHandoverReported }
                    .then { it.eventWithdrawalPeriodElapsed }
                    .then { it.serviceTaskActivateLeasing }
                    .then { it.endEventLeasingActive },
            )
            .hasPassedInOrder(
                ProcessPath.from(FlowNodes.GatewayFork)
                    .then { it.gatewayBikeSourceJoin }
                    .then { it.serviceTaskOrderBike }
                    .then { it.gatewayJoin },
            )
            .hasNotPassed(
                FlowNodes.EventBikeUnavailable.ELEMENT_ID,
                FlowNodes.EndEventApplicationRejected.ELEMENT_ID,
                FlowNodes.EndEventApplicationCancelled.ELEMENT_ID,
                FlowNodes.EndEventContractCancelled.ELEMENT_ID,
            )

        verify(exactly = 1) { sendContractUseCase.sendContract(id) }
        verify(exactly = 1) { issueInsurancePolicyUseCase.issuePolicy(id) }
        verify(exactly = 1) { activateLeasingUseCase.activate(id) }
    }

    @Test
    fun `escalation - contract not signed in time is escalated and rejected`() {
        val id = submit(age = 35, income = 3500.0)
        val instance = runtimeService.findProcessInstance(id)

        processEngine.continueToNextWaitState() // parks on the signature wait state

        processEngine.fireTimer(FlowNodes.EventSignatureDeadline) // deadline -> escalation -> rejection
        processEngine.continueToNextWaitState()

        assertThat(instance)
            .isEnded
            .hasPassedInOrder(
                pathUntilSignatureAwaited()
                    .then { it.eventSignatureDeadline }
                    .then { it.endEventNotSigned }
                    .interruptedBy(FlowNodes.SubProcessConcludeContract) { it.eventContractNotSigned }
                    .then { it.gatewayRejectionJoin }
                    .then { it.serviceTaskSendRejection }
                    .then { it.endEventApplicationRejected },
            )
            .hasNotPassed(FlowNodes.EndEventLeasingActive.ELEMENT_ID)

        verify(exactly = 1) { rejectApplicationUseCase.reject(id) }
    }

    @Test
    fun `not solvent - the DMN routes the application straight to rejection`() {
        // age below 18 cannot sign a leasing contract, so the DMN returns solvent = false
        val id = submit(age = 15, income = 3500.0)
        val instance = runtimeService.findProcessInstance(id)

        processEngine.continueToNextWaitState() // DMN -> not solvent -> rejection -> end

        assertThat(instance)
            .isEnded
            .hasPassedInOrder(
                pathUntilCreditRatingChecked()
                    .then { it.gatewayRejectionJoin }
                    .then { it.serviceTaskSendRejection }
                    .then { it.endEventApplicationRejected },
            )
            .hasNotPassed(FlowNodes.SubProcessConcludeContract.ELEMENT_ID)

        verify(exactly = 1) { rejectApplicationUseCase.reject(id) }
        verify(exactly = 0) { sendContractUseCase.sendContract(any()) }
    }

    @Test
    fun `abort - withdrawing the application compensates the completed steps`() {
        every { requestOrderCancellationUseCase.requestCancellation(any()) } returns true

        val id = submit(age = 35, income = 3500.0)
        val instance = runtimeService.findProcessInstance(id)

        // drive up to the handover wait state (contract signed, bike ordered, insured)
        processEngine.continueToNextWaitState()
        process.correlateContractSigned(id)
        processEngine.continueToNextWaitState()

        // Withdrawing triggers compensation; its handlers run in an engine-defined order, so drive the
        // continuations generically until the cancelBikeOrder sub-process parks on its user task.
        process.correlateApplicationWithdrawn(id)
        processEngine.continueToNextWaitState()

        val task =
            taskService
                .createTaskQuery()
                .taskDefinitionKey(CancelBikeOrderProcessApi.FlowNodes.UserTaskClarifyReturn.ELEMENT_ID)
                .singleResult()
        taskService.complete(task.id, mapOf("returnClarified" to true))
        processEngine.continueToNextWaitState()

        assertThat(instance)
            .isEnded
            .hasPassed(
                ProcessPath.from(FlowNodes.StartEventApplicationWithdrawn)
                    .then { it.eventReverseApplication }
                    .throwingCompensation(FlowNodes.EventCompensateContract) { it.serviceTaskCancelContract }
                    .throwingCompensation(FlowNodes.EventCompensateInsurance) { it.serviceTaskCancelPolicy }
                    .throwingCompensation(FlowNodes.EventCompensateOrder) { it.callActivityCancelBikeOrder }
                    .then { it.serviceTaskSendCancellationConfirmation }
                    .then { it.endEventApplicationCancelled },
            )
            .hasNotPassed(FlowNodes.EndEventLeasingActive.ELEMENT_ID)

        // External-task delivery is at-least-once: while compensation runs several handlers in
        // parallel, an async continuation firing on the test thread can force an optimistic-locking
        // retry of a handler that already ran, so a worker's use case may be invoked more than once
        // (workers are expected to be idempotent). The process still ends correctly, as asserted above.
        verify(atLeast = 1) { cancelContractUseCase.cancelContract(id) }
        verify(atLeast = 1) { cancelInsurancePolicyUseCase.cancelPolicy(id) }
        verify(atLeast = 1) { sendCancellationConfirmationUseCase.sendCancellationConfirmation(id) }
    }

    @Test
    fun `bike unavailable - clarifying an alternative re-orders and leasing becomes active`() {
        val id = submitUntilBikeUnavailable()
        val instance = runtimeService.findProcessInstance(id)

        every { orderBikeUseCase.orderBike(any()) } returns OrderId("ORDER-2") // the alternative is in stock
        // the alternative is clarified from the outside — the "external" completion of the user task
        process.completeAlternativeClarification(id, alternativeFound = true, bikeId = BikeId("BIKE-ALT"))
        processEngine.continueToNextWaitState() // re-order succeeds -> parallel join -> handover wait state

        process.correlateHandoverReported(id)
        processEngine.continueToNextWaitState()
        processEngine.fireTimer(FlowNodes.EventWithdrawalPeriodElapsed)
        processEngine.continueToNextWaitState()

        assertThat(instance)
            .isEnded
            .hasPassedInOrder(
                ProcessPath.from(FlowNodes.GatewayFork)
                    .then { it.gatewayBikeSourceJoin }
                    .then { it.serviceTaskOrderBike }
                    .interruptedBy(FlowNodes.ServiceTaskOrderBike) { it.eventBikeUnavailable }
                    .then { it.userTaskClarifyAlternative }
                    .then { it.gatewayAlternativeFound }
                    .then { it.gatewayBikeSourceJoin }
                    .then { it.serviceTaskOrderBike }
                    .then { it.gatewayJoin }
                    .then { it.eventHandoverReported }
                    .then { it.eventWithdrawalPeriodElapsed }
                    .then { it.serviceTaskActivateLeasing }
                    .then { it.endEventLeasingActive },
            )
            .hasNotPassed(
                FlowNodes.EndEventContractCancelled.ELEMENT_ID,
                FlowNodes.EndEventApplicationRejected.ELEMENT_ID,
            )

        verify(exactly = 2) { orderBikeUseCase.orderBike(id) }
    }

    @Test
    fun `bike unavailable - declining the alternative reverses contract and policy without cancelling an order`() {
        val id = submitUntilBikeUnavailable()
        val instance = runtimeService.findProcessInstance(id)

        process.completeAlternativeClarification(id, alternativeFound = false)
        processEngine.continueToNextWaitState() // -> compensation -> sendCancellationConfirmation -> end cancelled

        assertThat(instance)
            .isEnded
            .hasPassed(
                ProcessPath.from(FlowNodes.GatewayAlternativeFound)
                    .then { it.eventTriggerReversal }
                    .throwingCompensation(FlowNodes.EventCompensateContract) { it.serviceTaskCancelContract }
                    .throwingCompensation(FlowNodes.EventCompensateInsurance) { it.serviceTaskCancelPolicy }
                    .then { it.serviceTaskConfirmContractCancellation }
                    .then { it.endEventContractCancelled },
            )
            .hasNotPassed(
                FlowNodes.CallActivityCancelBikeOrder.ELEMENT_ID,
                FlowNodes.EndEventLeasingActive.ELEMENT_ID,
            )

        verify(atLeast = 1) { sendCancellationConfirmationUseCase.sendCancellationConfirmation(id) }
        verify(exactly = 0) { requestOrderCancellationUseCase.requestCancellation(any()) }
    }

    @Test
    fun `abort while clarifying an alternative - compensates contract and policy without cancelling an order`() {
        val id = submitUntilBikeUnavailable()
        val instance = runtimeService.findProcessInstance(id)

        process.correlateApplicationWithdrawn(id)
        processEngine.continueToNextWaitState() // -> compensation -> sendCancellationConfirmation -> end cancelled

        assertThat(instance)
            .isEnded
            .hasPassed(
                ProcessPath.from(FlowNodes.StartEventApplicationWithdrawn)
                    .then { it.eventReverseApplication }
                    .throwingCompensation(FlowNodes.EventCompensateContract) { it.serviceTaskCancelContract }
                    .throwingCompensation(FlowNodes.EventCompensateInsurance) { it.serviceTaskCancelPolicy }
                    .then { it.serviceTaskSendCancellationConfirmation }
                    .then { it.endEventApplicationCancelled },
            )
            .hasNotPassed(FlowNodes.CallActivityCancelBikeOrder.ELEMENT_ID)

        verify(exactly = 0) { requestOrderCancellationUseCase.requestCancellation(any()) }
    }

    @Test
    fun `abort after an accepted alternative - cancels the one placed order exactly once`() {
        every { requestOrderCancellationUseCase.requestCancellation(any()) } returns true

        val id = submitUntilBikeUnavailable()
        val instance = runtimeService.findProcessInstance(id)

        every { orderBikeUseCase.orderBike(any()) } returns OrderId("ORDER-2") // the alternative is in stock
        process.completeAlternativeClarification(id, alternativeFound = true, bikeId = BikeId("BIKE-ALT"))
        processEngine.continueToNextWaitState() // re-order succeeds -> parallel join -> handover wait state

        process.correlateApplicationWithdrawn(id)
        processEngine.continueToNextWaitState()

        val task =
            taskService
                .createTaskQuery()
                .taskDefinitionKey(CancelBikeOrderProcessApi.FlowNodes.UserTaskClarifyReturn.ELEMENT_ID)
                .singleResult()
        taskService.complete(task.id, mapOf("returnClarified" to true))
        processEngine.continueToNextWaitState()

        assertThat(instance).isEnded.hasPassed(FlowNodes.EndEventApplicationCancelled.ELEMENT_ID)
        val orderCancellations =
            historyService
                .createHistoricProcessInstanceQuery()
                .processDefinitionKey(CancelBikeOrderProcessApi.PROCESS_ID.value)
                .superProcessInstanceId(instance.id)
                .count()
        Assertions.assertThat(orderCancellations).isEqualTo(1)
    }

    /** Drives a signed, insured application to the clarify-alternative task: the dealer has no bike. */
    private fun submitUntilBikeUnavailable(): ApplicationId {
        every { orderBikeUseCase.orderBike(any()) } throws BikeUnavailableException(BikeId("BIKE-TEST"))

        val id = submit(age = 35, income = 3500.0)
        processEngine.continueToNextWaitState() // parks on the signature wait state
        process.correlateContractSigned(id)
        processEngine.continueToNextWaitState() // fork -> order raises bikeUnavailable -> parks on clarify-alternative
        return id
    }

    private fun pathUntilCreditRatingChecked() =
        ProcessPath.from(FlowNodes.StartEventLeasingRequestReceived)
            .then { it.businessRuleTaskCheckCreditRating }
            .then { it.gatewayIsSolvent }

    private fun pathUntilSignatureAwaited() =
        pathUntilCreditRatingChecked()
            .onto { it.subProcessConcludeContract }
            .enter { it.startEventCustomerEligible }
            .then { it.serviceTaskSendContract }
            .then { it.gatewayAwaitSignature }

    private fun pathUntilContractSigned() =
        pathUntilCreditRatingChecked()
            .onto { it.subProcessConcludeContract }
            .inside {
                enter { it.startEventCustomerEligible }
                    .then { it.serviceTaskSendContract }
                    .then { it.gatewayAwaitSignature }
                    .then { it.eventContractSigned }
                    .then { it.endEventContractValid }
            }

    private fun submit(age: Int, income: Double, bikeId: String = "BIKE-TEST"): ApplicationId {
        val application =
            LeasingApplication(
                id = ApplicationId.new(),
                customerName = CustomerName("Test Customer"),
                email = Email("test@example.com"),
                age = age,
                monthlyNetIncome = income,
                bikeId = BikeId(bikeId),
                status = LeasingStatus.RECEIVED,
                createdAt = LocalDateTime.now(),
            )
        process.submitRequest(application)
        return application.id
    }
}
