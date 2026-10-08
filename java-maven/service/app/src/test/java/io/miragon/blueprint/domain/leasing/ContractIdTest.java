package io.miragon.blueprint.domain.leasing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ContractIdTest {

    @Test
    void exposes_the_wrapped_contract_id() {
        // given/when: a contract id is created from a non-blank value
        ContractId contractId = new ContractId("CONTRACT-1");
        // then: the raw value is exposed unchanged
        assertThat(contractId.value()).isEqualTo("CONTRACT-1");
    }

    @Test
    void rejects_a_blank_contract_id() {
        // when/then: a blank value is refused
        assertThatThrownBy(() -> new ContractId("   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("ContractId must not be blank");
    }

    @Test
    void rejects_a_contract_id_of_non_breaking_spaces() {
        // when/then: a value of only non-breaking spaces (U+00A0, U+2007, U+202F) is blank too
        assertThatThrownBy(() -> new ContractId("\u00A0\u2007\u202F"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("ContractId must not be blank");
    }
}
