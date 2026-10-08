package io.miragon.blueprint.process.util

import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.PROCESS_ID
import io.miragon.blueprint.domain.leasing.ApplicationId
import org.assertj.core.api.Assertions.assertThat
import org.cibseven.bpm.engine.RuntimeService
import org.cibseven.bpm.engine.runtime.ProcessInstance

/**
 * Finds the bike-leasing process instance for the given application id (used as the business key).
 * Fails the test if no such instance exists.
 */
fun RuntimeService.findProcessInstance(id: ApplicationId): ProcessInstance {
    val instance =
        createProcessInstanceQuery()
            .processDefinitionKey(PROCESS_ID.value)
            .processInstanceBusinessKey(id.value.toString())
            .singleResult()
    assertThat(instance).`as`("process instance for application %s", id.value).isNotNull
    return instance
}
