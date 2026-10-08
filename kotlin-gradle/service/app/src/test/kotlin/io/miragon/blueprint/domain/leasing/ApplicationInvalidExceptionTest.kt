package io.miragon.blueprint.domain.leasing

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class ApplicationInvalidExceptionTest {

    @Test
    fun `carries the reason and a message naming the application`() {
        // given: an application id and a rejection reason
        val id = ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))
        // when: the exception is raised
        val exception = ApplicationInvalidException(id, "no income")
        // then: reason and composed message are exposed
        assertThat(exception.reason).isEqualTo("no income")
        assertThat(exception.applicationId).isEqualTo(id)
        assertThat(exception.message)
            .isEqualTo("Application 123e4567-e89b-12d3-a456-426614174000 is invalid: no income")
    }
}
