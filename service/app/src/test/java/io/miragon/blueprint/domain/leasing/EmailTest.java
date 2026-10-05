package io.miragon.blueprint.domain.leasing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class EmailTest {

    @Test
    void exposes_a_valid_email_address() {
        // given/when: an email is created from a well-formed address
        Email email = new Email("john.doe@test.com");
        // then: the raw value is exposed unchanged
        assertThat(email.value()).isEqualTo("john.doe@test.com");
    }

    @Test
    void rejects_a_malformed_address() {
        // when/then: a value that is not an email is refused
        assertThatThrownBy(() -> new Email("not-an-email"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'not-an-email' is not a valid email address");
    }

    @Test
    void rejects_an_empty_address() {
        // when/then: an empty value is refused
        assertThatThrownBy(() -> new Email(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'' is not a valid email address");
    }
}
