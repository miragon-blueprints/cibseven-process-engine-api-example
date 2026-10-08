package io.miragon.blueprint.domain.bike

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class OrderIdTest {

    @Test
    fun `exposes the wrapped order id`() {
        // given/when: an order id is created from a non-blank value
        val orderId = OrderId("ORDER-1")
        // then: the raw value is exposed unchanged
        assertThat(orderId.value).isEqualTo("ORDER-1")
    }

    @Test
    fun `rejects a blank order id`() {
        // when/then: a blank value is refused
        assertThatThrownBy { OrderId("   ") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `rejects an order id of non-breaking spaces`() {
        // when/then: a value of only non-breaking spaces (U+00A0, U+2007, U+202F) is blank too
        assertThatThrownBy { OrderId("\u00A0\u2007\u202F") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
