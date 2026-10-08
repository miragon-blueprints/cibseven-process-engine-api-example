package io.miragon.blueprint.domain.bike;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class BikeIdTest {

    @Test
    void exposes_the_wrapped_bike_id() {
        // given/when: a bike id is created from a non-blank value
        BikeId bikeId = new BikeId("BIKE-900");
        // then: the raw value is exposed unchanged
        assertThat(bikeId.value()).isEqualTo("BIKE-900");
    }

    @Test
    void rejects_a_blank_bike_id() {
        // when/then: a blank value is refused
        assertThatThrownBy(() -> new BikeId("   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("BikeId must not be blank");
    }

    @Test
    void rejects_a_bike_id_of_non_breaking_spaces() {
        // when/then: a value of only non-breaking spaces (U+00A0, U+2007, U+202F) is blank too
        assertThatThrownBy(() -> new BikeId("\u00A0\u2007\u202F"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("BikeId must not be blank");
    }
}
