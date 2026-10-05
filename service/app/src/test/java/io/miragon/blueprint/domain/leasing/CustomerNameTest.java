package io.miragon.blueprint.domain.leasing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CustomerNameTest {

    @Test
    void exposes_the_wrapped_name() {
        // given/when: a customer name is created from a non-blank value
        CustomerName name = new CustomerName("John Doe");
        // then: the raw value is exposed unchanged
        assertThat(name.value()).isEqualTo("John Doe");
    }

    @Test
    void rejects_an_empty_name() {
        // when/then: an empty value is refused
        assertThatThrownBy(() -> new CustomerName(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Customer name must not be blank");
    }

    @Test
    void rejects_a_blank_name() {
        // when/then: a whitespace-only value is refused
        assertThatThrownBy(() -> new CustomerName("   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Customer name must not be blank");
    }

    @Test
    void rejects_a_name_of_non_breaking_spaces() {
        // when/then: a value of only non-breaking spaces (U+00A0, U+2007, U+202F) is blank too
        assertThatThrownBy(() -> new CustomerName("\u00A0\u2007\u202F"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Customer name must not be blank");
    }
}
