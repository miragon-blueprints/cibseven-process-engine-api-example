package io.miragon.blueprint.adapter.outbound.contract;

import static io.miragon.blueprint.domain.leasing.TestObjectBuilder.testLeasingApplication;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.miragon.blueprint.domain.leasing.ContractId;
import org.junit.jupiter.api.Test;

class LoggingContractAdapterTest {

    private final LoggingContractAdapter underTest = new LoggingContractAdapter();

    @Test
    void issueContract_mints_a_fresh_contract_id() {
        // given: an application / when: a contract is issued twice
        ContractId first = underTest.issueContract(testLeasingApplication().build().id());
        ContractId second = underTest.issueContract(testLeasingApplication().build().id());
        // then: each issue gets its own reference
        assertThat(first.value()).startsWith("CONTRACT-");
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void revokeContract_logs_without_error() {
        // given: an issued contract / when-then: revoking it runs without error
        assertThatCode(() -> underTest.revokeContract(new ContractId("CONTRACT-1"))).doesNotThrowAnyException();
    }
}
