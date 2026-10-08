package io.miragon.blueprint.domain.bike;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class OrderIdTest {

    @Test
    void exposes_the_wrapped_order_id() {
        // given/when: an order id is created from a non-blank value
        OrderId orderId = new OrderId("ORDER-1");
        // then: the raw value is exposed unchanged
        assertThat(orderId.value()).isEqualTo("ORDER-1");
    }

    @Test
    void rejects_a_blank_order_id() {
        // when/then: a blank value is refused
        assertThatThrownBy(() -> new OrderId("   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("OrderId must not be blank");
    }

    @Test
    void rejects_an_order_id_of_non_breaking_spaces() {
        // when/then: a value of only non-breaking spaces (U+00A0, U+2007, U+202F) is blank too
        assertThatThrownBy(() -> new OrderId("\u00A0\u2007\u202F"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("OrderId must not be blank");
    }
}
