package io.miragon.blueprint.domain.leasing

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ContractIdTest {

    @Test
    fun `exposes the wrapped contract id`() {
        // given/when: a contract id is created from a non-blank value
        val contractId = ContractId("CONTRACT-1")
        // then: the raw value is exposed unchanged
        assertThat(contractId.value).isEqualTo("CONTRACT-1")
    }

    @Test
    fun `rejects a blank contract id`() {
        // when/then: a blank value is refused
        assertThatThrownBy { ContractId("   ") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `rejects a contract id of non-breaking spaces`() {
        // when/then: a value of only non-breaking spaces (U+00A0, U+2007, U+202F) is blank too
        assertThatThrownBy { ContractId("\u00A0\u2007\u202F") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
