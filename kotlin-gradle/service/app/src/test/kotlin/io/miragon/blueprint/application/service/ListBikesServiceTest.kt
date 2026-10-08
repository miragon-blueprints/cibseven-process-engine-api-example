package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.outbound.BikeDealerPort
import io.miragon.blueprint.application.port.outbound.BikePortfolioRepository
import io.miragon.blueprint.domain.bike.Bike
import io.miragon.blueprint.domain.bike.BikeId
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ListBikesServiceTest {

    private val bikePortfolio = mockk<BikePortfolioRepository>()
    private val bikeDealer = mockk<BikeDealerPort>()
    private val underTest = ListBikesService(bikePortfolio, bikeDealer)

    @Test
    fun `enriches each catalogue bike with its dealer availability`() {
        // given: a catalogue (already ordered by the portfolio) where one bike is out of stock
        every { bikePortfolio.findAll() } returns
            listOf(
                Bike(BikeId("BIKE-900"), "Gravel Explorer 900"),
                Bike(BikeId("BIKE-OOS"), "Mountain Trail 600"),
            )
        every { bikeDealer.checkAvailability(BikeId("BIKE-900")) } returns true
        every { bikeDealer.checkAvailability(BikeId("BIKE-OOS")) } returns false

        // when: the catalogue is listed
        val result = underTest.all()

        // then: the portfolio's order is preserved and each item's availability comes from the dealer
        assertThat(result.map { it.bikeId.value }).containsExactly("BIKE-900", "BIKE-OOS")
        assertThat(result.single { it.bikeId.value == "BIKE-900" }.available).isTrue()
        assertThat(result.single { it.bikeId.value == "BIKE-OOS" }.available).isFalse()
    }

    @Test
    fun `returns an empty list for an empty catalogue`() {
        // given: no bikes
        every { bikePortfolio.findAll() } returns emptyList()

        // when / then: nothing is listed and the dealer is never consulted
        assertThat(underTest.all()).isEmpty()
    }
}
