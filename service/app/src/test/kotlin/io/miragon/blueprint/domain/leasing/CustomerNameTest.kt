package io.miragon.blueprint.domain.leasing

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class CustomerNameTest {

    @Test
    fun `exposes the wrapped name`() {
        // given/when: a customer name is created from a non-blank value
        val name = CustomerName("John Doe")
        // then: the raw value is exposed unchanged
        assertThat(name.value).isEqualTo("John Doe")
    }

    @Test
    fun `rejects an empty name`() {
        // when/then: an empty value is refused
        assertThatThrownBy { CustomerName("") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `rejects a blank name`() {
        // when/then: a whitespace-only value is refused
        assertThatThrownBy { CustomerName("   ") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
