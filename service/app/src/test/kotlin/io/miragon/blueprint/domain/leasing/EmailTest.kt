package io.miragon.blueprint.domain.leasing

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class EmailTest {

    @Test
    fun `exposes a valid email address`() {
        // given/when: an email is created from a well-formed address
        val email = Email("john.doe@test.com")
        // then: the raw value is exposed unchanged
        assertThat(email.value).isEqualTo("john.doe@test.com")
    }

    @Test
    fun `rejects a malformed address`() {
        // when/then: a value that is not an email is refused
        assertThatThrownBy { Email("not-an-email") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `rejects an empty address`() {
        // when/then: an empty value is refused
        assertThatThrownBy { Email("") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
