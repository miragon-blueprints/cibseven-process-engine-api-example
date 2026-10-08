package io.miragon.blueprint.domain.leasing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApplicationIdTest {

    @Test
    void newId_creates_a_fresh_random_id() {
        // when: two ids are generated
        ApplicationId first = ApplicationId.newId();
        ApplicationId second = ApplicationId.newId();
        // then: each wraps a UUID and they differ
        assertThat(first.value()).isNotNull();
        assertThat(second.value()).isNotNull();
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void of_parses_the_textual_uuid() {
        // given/when: an id is parsed from its textual form
        ApplicationId id = ApplicationId.of("123e4567-e89b-12d3-a456-426614174000");
        // then: it wraps exactly that UUID
        assertThat(id.value()).isEqualTo(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
    }

    @Test
    void of_rejects_a_malformed_uuid() {
        // when/then: a value that is not a UUID is refused
        assertThatThrownBy(() -> ApplicationId.of("not-a-uuid"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
