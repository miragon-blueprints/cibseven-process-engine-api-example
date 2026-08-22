package io.miragon.blueprint.domain.bike

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class BikeIdTest {

    @Test
    fun `exposes the wrapped bike id`() {
        // given/when: a bike id is created from a non-blank value
        val bikeId = BikeId("BIKE-900")
        // then: the raw value is exposed unchanged
        assertThat(bikeId.value).isEqualTo("BIKE-900")
    }

    @Test
    fun `rejects a blank bike id`() {
        // when/then: a blank value is refused
        assertThatThrownBy { BikeId("   ") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
