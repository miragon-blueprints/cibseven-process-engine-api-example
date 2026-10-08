package io.miragon.blueprint.process.model;

import io.miragon.bpmn.domain.shared.ProcessEngine;
import io.miragon.bpmn.domain.validation.SingleModelValidationRule;
import io.miragon.bpmn.testing.BpmnRules;
import io.miragon.bpmn.testing.BpmnValidator;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Validates the BPMN models themselves (structure, not behaviour) with the {@code bpmn-to-code-testing}
 * rule engine: all built-in rules ({@link BpmnRules#all()}) plus the custom {@link ServiceTaskExternalTopicRule}.
 * Runs at build time from the classpath — no engine required.
 */
class BikeLeasingModelValidationTest {

    @Test
    void the_bpmn_models_satisfy_all_rules_and_only_use_external_task_topics() {
        List<SingleModelValidationRule> rules = new ArrayList<>(BpmnRules.all());
        rules.add(new ServiceTaskExternalTopicRule());

        BpmnValidator
            .fromClasspath("bpmn/")
            .engine(ProcessEngine.CAMUNDA_7)
            .withRules(rules)
            .validate()
            .assertNoViolations();
    }
}
