package io.miragon.blueprint.process

import com.ninjasquad.springmockk.MockkBean
import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.Elements
import io.miragon.blueprint.adapter.process.CancelBikeOrderProcessApi
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
import io.miragon.blueprint.application.port.inbound.ValidateApplicationUseCase
import io.miragon.blueprint.application.port.outbound.LeasingProcess
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.leasing.CustomerName
import io.miragon.blueprint.domain.leasing.Email
import io.miragon.blueprint.domain.leasing.LeasingApplication
import io.miragon.blueprint.domain.leasing.LeasingStatus
import io.miragon.blueprint.domain.bike.OrderId
import io.miragon.blueprint.process.util.continueToNextWaitState
import io.miragon.blueprint.process.util.findProcessInstance
import io.miragon.blueprint.process.util.fireTimer
import io.mockk.every
import io.mockk.verify
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
    private lateinit var processEngine: ProcessEngine

    @MockkBean(relaxed = true)
    private lateinit var validateApplicationUseCase: ValidateApplicationUseCase

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

    @BeforeEach
    fun setUp() {
        init(processEngine)
        every { orderBikeUseCase.orderBike(any()) } returns
            OrderBikeUseCase.Result(OrderId("ORDER-1"), bikeAvailable = true)
    }

    @Test
    fun `happy path - contract signed, bike available, leasing becomes active`() {
        val id = submit(age = 35, income = 3500.0)
        val instance = runtimeService.findProcessInstance(id)

        // validate -> DMN -> conclude-contract sub-process parks on the signature wait state
        processEngine.continueToNextWaitState()

        process.correlateContractSigned(id) // forks into insurance + bike order, joins -> handover wait state
        processEngine.continueToNextWaitState()

        process.correlateHandoverReported(id) // -> withdrawal-period timer
        processEngine.continueToNextWaitState()

        processEngine.fireTimer(Elements.EVENT_WITHDRAWAL_PERIOD_ELAPSED)
        processEngine.continueToNextWaitState()

        assertThat(instance)
            .isEnded
            .hasPassedInOrder(
                Elements.SERVICE_TASK_VALIDATE_APPLICATION.value,
                Elements.BUSINESS_RULE_TASK_CHECK_CREDIT_RATING.value,
                Elements.SERVICE_TASK_SEND_CONTRACT.value,
                Elements.SERVICE_TASK_ISSUE_INSURANCE_POLICY.value,
                Elements.EVENT_HANDOVER_REPORTED.value,
                Elements.END_EVENT_LEASING_ACTIVE.value,
            )
            .hasNotPassed(
                Elements.END_EVENT_APPLICATION_REJECTED.value,
                Elements.END_EVENT_APPLICATION_CANCELLED.value,
                Elements.END_EVENT_CONTRACT_CANCELLED.value,
            )

        verify(exactly = 1) { sendContractUseCase.sendContract(id) }
        verify(exactly = 1) { issueInsurancePolicyUseCase.issuePolicy(id) }
    }

    @Test
    fun `escalation - contract not signed in time is escalated and rejected`() {
        val id = submit(age = 35, income = 3500.0)
        val instance = runtimeService.findProcessInstance(id)

        processEngine.continueToNextWaitState() // parks on the signature wait state

        processEngine.fireTimer(Elements.EVENT_SIGNATURE_DEADLINE) // deadline -> escalation -> rejection
        processEngine.continueToNextWaitState()

        assertThat(instance)
            .isEnded
            .hasPassed(
                Elements.EVENT_SIGNATURE_DEADLINE.value,
                Elements.BOUNDARY_CONTRACT_NOT_SIGNED.value,
                Elements.SERVICE_TASK_SEND_REJECTION.value,
                Elements.END_EVENT_APPLICATION_REJECTED.value,
            )
            .hasNotPassed(Elements.END_EVENT_LEASING_ACTIVE.value)

        verify(exactly = 1) { rejectApplicationUseCase.reject(id) }
    }

    @Test
    fun `not solvent - the DMN routes the application straight to rejection`() {
        // age below 18 cannot sign a leasing contract, so the DMN returns solvent = false
        val id = submit(age = 15, income = 3500.0)

        processEngine.continueToNextWaitState() // validate -> DMN -> not solvent -> rejection -> end

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
                .taskDefinitionKey(CancelBikeOrderProcessApi.Elements.USER_TASK_CLARIFY_RETURN.value)
                .singleResult()
        taskService.complete(task.id, mapOf("returnClarified" to true))
        processEngine.continueToNextWaitState()

        assertThat(instance)
            .isEnded
            .hasPassed(
                Elements.SERVICE_TASK_CANCEL_CONTRACT.value,
                Elements.SERVICE_TASK_CANCEL_POLICY.value,
                Elements.CALL_ACTIVITY_CANCEL_BIKE_ORDER.value,
                Elements.SERVICE_TASK_SEND_CANCELLATION_CONFIRMATION.value,
                Elements.END_EVENT_APPLICATION_CANCELLED.value,
            )
            .hasNotPassed(Elements.END_EVENT_LEASING_ACTIVE.value)

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
        // the first order finds the requested bike unavailable, the re-order after the alternative succeeds
        every { orderBikeUseCase.orderBike(any()) } returnsMany
            listOf(
                OrderBikeUseCase.Result(orderId = null, bikeAvailable = false),
                OrderBikeUseCase.Result(OrderId("ORDER-2"), bikeAvailable = true),
            )

        val id = submit(age = 35, income = 3500.0)
        val instance = runtimeService.findProcessInstance(id)

        processEngine.continueToNextWaitState() // parks on the signature wait state
        process.correlateContractSigned(id)
        processEngine.continueToNextWaitState() // fork -> order finds bike unavailable -> parks on clarify-alternative

        // the alternative is clarified from the outside — the "external" completion of the user task
        process.completeAlternativeClarification(id, alternativeFound = true, bikeId = BikeId("BIKE-ALT"))
        processEngine.continueToNextWaitState() // re-order succeeds -> parallel join -> handover wait state

        process.correlateHandoverReported(id)
        processEngine.continueToNextWaitState()
        processEngine.fireTimer(Elements.EVENT_WITHDRAWAL_PERIOD_ELAPSED)
        processEngine.continueToNextWaitState()

        assertThat(instance)
            .isEnded
            .hasPassed(
                Elements.USER_TASK_CLARIFY_ALTERNATIVE.value,
                Elements.SERVICE_TASK_ORDER_BIKE.value,
                Elements.END_EVENT_LEASING_ACTIVE.value,
            )
            .hasNotPassed(
                Elements.END_EVENT_CONTRACT_CANCELLED.value,
                Elements.END_EVENT_APPLICATION_REJECTED.value,
            )

        verify(exactly = 2) { orderBikeUseCase.orderBike(id) }
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
