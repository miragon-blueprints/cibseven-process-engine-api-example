package io.miragon.blueprint.process

import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.FlowNodes
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.application.port.outbound.LeasingProcess
import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.CustomerName
import io.miragon.blueprint.domain.leasing.Email
import io.miragon.blueprint.domain.leasing.LeasingApplication
import io.miragon.blueprint.domain.leasing.LeasingStatus
import io.miragon.blueprint.process.util.continueToNextWaitState
import io.miragon.blueprint.process.util.findProcessInstance
import org.assertj.core.api.Assertions
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
 * Runs the out-of-stock order with the **real** use cases instead of mocks. The `@Transactional`
 * order service throws the `BikeUnavailableException` out of its transaction; that must not keep the
 * `@ProcessEngineWorker` from reporting the `bikeUnavailable` BPMN error to the engine — otherwise the
 * order task would fail into an incident — and it must not undo the bike the service stored before. [BikeLeasingProcessTest] mocks the use cases
 * and therefore cannot see this.
 */
@SpringBootTest
@ActiveProfiles("test")
class BikeUnavailableTransactionTest {

    @Autowired
    private lateinit var process: LeasingProcess

    @Autowired
    private lateinit var repository: LeasingApplicationRepository

    @Autowired
    private lateinit var runtimeService: RuntimeService

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var processEngine: ProcessEngine

    @BeforeEach
    fun setUp() {
        init(processEngine)
    }

    @Test
    fun `bike unavailable - the BPMN error raised by the real order service reaches the engine`() {
        // BIKE-OOS is on the simulated dealer's out-of-stock list
        val application =
            LeasingApplication.receive(
                id = ApplicationId.new(),
                customerName = CustomerName("Test Customer"),
                email = Email("test@example.com"),
                age = 35,
                monthlyNetIncome = 3500.0,
                bikeId = BikeId("BIKE-OOS"),
                createdAt = LocalDateTime.now(),
            )
        repository.save(application)
        process.submitRequest(application)
        val instance = runtimeService.findProcessInstance(application.id)

        processEngine.continueToNextWaitState() // parks on the signature wait state
        process.correlateContractSigned(application.id)
        processEngine.continueToNextWaitState() // fork -> order raises bikeUnavailable -> parks on clarify-alternative

        assertThat(instance)
            .isWaitingAt(FlowNodes.UserTaskClarifyAlternative.ELEMENT_ID)
            .hasPassed(FlowNodes.EventBikeUnavailable.ELEMENT_ID)
        Assertions.assertThat(runtimeService.createIncidentQuery().processInstanceId(instance.id).count()).isZero()
    }

    @Test
    fun `alternative via tasklist - the bike the form submits is ordered and stored on the application`() {
        val application = receivedApplication(BikeId("BIKE-OOS"))
        repository.save(application)
        process.submitRequest(application)
        val instance = runtimeService.findProcessInstance(application.id)
        signContractAndRunIntoTheUnavailableBike(application.id)

        // the Tasklist completes the task with the variables its Camunda Form submits, bypassing the domain endpoint
        val task =
            taskService
                .createTaskQuery()
                .processInstanceId(instance.id)
                .taskDefinitionKey(FlowNodes.UserTaskClarifyAlternative.ELEMENT_ID)
                .singleResult()
        taskService.complete(
            task.id,
            mapOf(
                FlowNodes.UserTaskClarifyAlternative.Variables.ALTERNATIVE_FOUND.value to true,
                FlowNodes.UserTaskClarifyAlternative.Variables.BIKE_ID.value to "BIKE-900",
            ),
        )
        processEngine.continueToNextWaitState() // re-order succeeds -> parallel join -> handover wait state

        assertThat(instance).isWaitingAt(FlowNodes.EventHandoverReported.ELEMENT_ID)
        val stored = repository.findById(application.id)
        Assertions.assertThat(stored?.bikeId).isEqualTo(BikeId("BIKE-900"))
        Assertions.assertThat(stored?.status).isEqualTo(LeasingStatus.ORDERED)
    }

    @Test
    fun `bike unavailable - the bike the process carries stays on the application although the order fails`() {
        val application = receivedApplication(BikeId("BIKE-900"))
        repository.save(application)
        process.submitRequest(application.selectAlternative(BikeId("BIKE-OOS")))
        val instance = runtimeService.findProcessInstance(application.id)

        signContractAndRunIntoTheUnavailableBike(application.id)

        assertThat(instance).isWaitingAt(FlowNodes.UserTaskClarifyAlternative.ELEMENT_ID)
        Assertions.assertThat(repository.findById(application.id)?.bikeId).isEqualTo(BikeId("BIKE-OOS"))
    }

    private fun signContractAndRunIntoTheUnavailableBike(id: ApplicationId) {
        processEngine.continueToNextWaitState() // parks on the signature wait state
        process.correlateContractSigned(id)
        processEngine.continueToNextWaitState() // fork -> order raises bikeUnavailable -> parks on clarify-alternative
    }

    private fun receivedApplication(bikeId: BikeId) =
        LeasingApplication.receive(
            id = ApplicationId.new(),
            customerName = CustomerName("Test Customer"),
            email = Email("test@example.com"),
            age = 35,
            monthlyNetIncome = 3500.0,
            bikeId = bikeId,
            createdAt = LocalDateTime.now(),
        )
}
