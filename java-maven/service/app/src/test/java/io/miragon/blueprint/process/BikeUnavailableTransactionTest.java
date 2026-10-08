package io.miragon.blueprint.process;

import static io.miragon.blueprint.process.util.JobExecutionUtils.continueToNextWaitState;
import static io.miragon.blueprint.process.util.ProcessInstanceUtils.findProcessInstance;
import static org.cibseven.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.cibseven.bpm.engine.test.assertions.bpmn.BpmnAwareTests.init;

import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.FlowNodes;
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository;
import io.miragon.blueprint.application.port.outbound.LeasingProcess;
import io.miragon.blueprint.domain.bike.BikeId;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import io.miragon.blueprint.domain.leasing.CustomerName;
import io.miragon.blueprint.domain.leasing.Email;
import io.miragon.blueprint.domain.leasing.LeasingApplication;
import java.time.LocalDateTime;
import org.assertj.core.api.Assertions;
import org.cibseven.bpm.engine.ProcessEngine;
import org.cibseven.bpm.engine.RuntimeService;
import org.cibseven.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Runs the out-of-stock order with the <strong>real</strong> use cases instead of mocks. The
 * {@code @Transactional} order service rolls its transaction back when it throws the
 * {@code BikeUnavailableException}; that must not keep the {@code @ProcessEngineWorker} from reporting the
 * {@code bikeUnavailable} BPMN error to the engine — otherwise the order task would fail into an incident.
 * {@link BikeLeasingProcessTest} mocks the use cases and therefore cannot see this.
 */
@SpringBootTest
@ActiveProfiles("test")
class BikeUnavailableTransactionTest {

    @Autowired
    private LeasingProcess process;

    @Autowired
    private LeasingApplicationRepository repository;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private ProcessEngine processEngine;

    @BeforeEach
    void setUp() {
        init(processEngine);
    }

    @Test
    void bike_unavailable_the_BPMN_error_raised_by_the_real_order_service_reaches_the_engine()
        throws InterruptedException {
        // BIKE-OOS is on the simulated dealer's out-of-stock list
        LeasingApplication application =
            LeasingApplication.receive(
                ApplicationId.newId(),
                new CustomerName("Test Customer"),
                new Email("test@example.com"),
                35,
                3500.0,
                new BikeId("BIKE-OOS"),
                LocalDateTime.now());
        repository.save(application);
        process.submitRequest(application);
        ProcessInstance instance = findProcessInstance(runtimeService, application.id());

        continueToNextWaitState(processEngine); // parks on the signature wait state
        process.correlateContractSigned(application.id());
        continueToNextWaitState(processEngine); // fork -> order raises bikeUnavailable -> parks on clarify-alternative

        assertThat(instance)
            .isWaitingAt(FlowNodes.UserTaskClarifyAlternative.ELEMENT_ID)
            .hasPassed(FlowNodes.EventBikeUnavailable.ELEMENT_ID);
        Assertions.assertThat(runtimeService.createIncidentQuery().processInstanceId(instance.getId()).count())
            .isZero();
    }
}
