package io.miragon.blueprint.domain.leasing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApplicationInvalidExceptionTest {

    @Test
    void carries_the_reason_and_a_message_naming_the_application() {
        // given: an application id and a rejection reason
        ApplicationId id = new ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        // when: the exception is raised
        ApplicationInvalidException exception = new ApplicationInvalidException(id, "no income");
        // then: reason and composed message are exposed
        assertThat(exception.getReason()).isEqualTo("no income");
        assertThat(exception.getApplicationId()).isEqualTo(id);
        assertThat(exception.getMessage())
            .isEqualTo("Application 123e4567-e89b-12d3-a456-426614174000 is invalid: no income");
    }
}
