package io.miragon.blueprint.process.util;

import static io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.PROCESS_ID;
import static org.assertj.core.api.Assertions.assertThat;

import io.miragon.blueprint.domain.leasing.ApplicationId;
import org.cibseven.bpm.engine.RuntimeService;
import org.cibseven.bpm.engine.runtime.ProcessInstance;

/**
 * Process-test helper for looking up process instances.
 */
public final class ProcessInstanceUtils {

    private ProcessInstanceUtils() {
    }

    /**
     * Finds the bike-leasing process instance for the given application id (used as the business key).
     * Fails the test if no such instance exists.
     */
    public static ProcessInstance findProcessInstance(RuntimeService runtimeService, ApplicationId id) {
        ProcessInstance instance =
            runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(PROCESS_ID.getValue())
                .processInstanceBusinessKey(id.value().toString())
                .singleResult();
        assertThat(instance).as("process instance for application %s", id.value()).isNotNull();
        return instance;
    }
}
