package io.miragon.blueprint.domain.leasing

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.UUID

class ApplicationIdTest {

    @Test
    fun `a new id is fresh and random`() {
        // when: two ids are generated
        val first = ApplicationId.new()
        val second = ApplicationId.new()
        // then: each wraps a UUID and they differ
        assertThat(first.value).isNotNull()
        assertThat(second.value).isNotNull()
        assertThat(first).isNotEqualTo(second)
    }

    @Test
    fun `of parses the textual uuid`() {
        // given/when: an id is parsed from its textual form
        val id = ApplicationId.of("123e4567-e89b-12d3-a456-426614174000")
        // then: it wraps exactly that UUID
        assertThat(id.value).isEqualTo(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))
    }

    @Test
    fun `of rejects a malformed uuid`() {
        // when/then: a value that is not a UUID is refused
        assertThatThrownBy { ApplicationId.of("not-a-uuid") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `the textual form names the type and the value`() {
        // given: an id, as the services interpolate it into the detail of a 404 response
        val id = ApplicationId.of("123e4567-e89b-12d3-a456-426614174000")
        // when/then: both variants render it identically
        assertThat(id.toString()).isEqualTo("ApplicationId(value=123e4567-e89b-12d3-a456-426614174000)")
    }
}
