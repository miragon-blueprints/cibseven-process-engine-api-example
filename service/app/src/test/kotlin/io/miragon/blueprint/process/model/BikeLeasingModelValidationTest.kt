package io.miragon.blueprint.process.model

import io.miragon.bpmn.domain.ProcessModel
import io.miragon.bpmn.domain.shared.ProcessEngine
import io.miragon.bpmn.domain.shared.ServiceTaskDefinition
import io.miragon.bpmn.domain.validation.model.SingleModelValidationContext
import io.miragon.bpmn.testing.BpmnRules
import io.miragon.bpmn.testing.BpmnValidator
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Validates the BPMN models themselves (structure, not behaviour) with the `bpmn-to-code-testing`
 * rule engine: all built-in rules ([BpmnRules.all]) plus the custom [ServiceTaskExternalTopicRule].
 * Runs at build time from the classpath — no engine required.
 */
class BikeLeasingModelValidationTest {

    @Test
    fun `the bpmn models satisfy all rules and only use external task topics`() {
        BpmnValidator
            .fromClasspath("bpmn/")
            .engine(ProcessEngine.CAMUNDA_7)
            .withRules(BpmnRules.all() + ServiceTaskExternalTopicRule())
            .validate()
            .assertNoViolations()
    }

}
