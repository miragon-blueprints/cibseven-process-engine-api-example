package io.miragon.blueprint.adapter.outbound.insurance;

import static io.miragon.blueprint.domain.leasing.TestObjectBuilder.testLeasingApplication;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

class LoggingInsuranceAdapterTest {

    private final LoggingInsuranceAdapter underTest = new LoggingInsuranceAdapter();

    @Test
    void issuePolicy_logs_without_error() {
        // given: an application / when-then: issuing the policy runs without error
        assertThatCode(() -> underTest.issuePolicy(testLeasingApplication().build().id())).doesNotThrowAnyException();
    }

    @Test
    void cancelPolicy_logs_without_error() {
        // given: an application / when-then: cancelling the policy runs without error
        assertThatCode(() -> underTest.cancelPolicy(testLeasingApplication().build().id())).doesNotThrowAnyException();
    }
}
