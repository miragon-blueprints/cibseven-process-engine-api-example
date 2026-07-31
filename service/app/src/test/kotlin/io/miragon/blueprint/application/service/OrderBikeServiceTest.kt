package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.outbound.BikeDealerPort
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.leasing.LeasingStatus
import io.miragon.blueprint.domain.bike.OrderId
import io.miragon.blueprint.domain.leasing.testLeasingApplication
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OrderBikeServiceTest {

    private val repository = mockk<LeasingApplicationRepository>()
    private val bikeDealer = mockk<BikeDealerPort>()
    private val underTest = OrderBikeService(repository = repository, bikeDealer = bikeDealer)

    @Test
    fun `orderBike places an order when the dealer has the bike in stock`() {

        // given: an application whose bike is available at the dealer
        val application = testLeasingApplication(bikeId = BikeId("BIKE-900"))
        every { repository.findById(application.id) } returns application
        every { bikeDealer.checkAvailability(application.bikeId) } returns true
        every { bikeDealer.order(application.bikeId) } returns OrderId("ORDER-900")
        every { repository.save(any()) } answers { firstArg() }

        // when: the bike is ordered
        val result = underTest.orderBike(application.id)

        // then: the order id is returned and the application moves to ORDERED
        assertThat(result.bikeAvailable).isTrue()
        assertThat(result.orderId).isEqualTo(OrderId("ORDER-900"))
        verify { bikeDealer.checkAvailability(application.bikeId) }
        verify { bikeDealer.order(application.bikeId) }
        verify { repository.save(match { it.status == LeasingStatus.ORDERED && it.orderId == OrderId("ORDER-900") }) }
        confirmVerified(bikeDealer)
    }

    @Test
    fun `orderBike reports an out-of-stock bike as unavailable and places no order`() {

        // given: an application whose bike is out of stock at the dealer
        val application = testLeasingApplication(bikeId = BikeId("BIKE-OOS"))
        every { repository.findById(application.id) } returns application
        every { bikeDealer.checkAvailability(application.bikeId) } returns false

        // when: the bike is ordered
        val result = underTest.orderBike(application.id)

        // then: no order is placed and the bike is reported unavailable
        assertThat(result.bikeAvailable).isFalse()
        assertThat(result.orderId).isNull()
        verify { bikeDealer.checkAvailability(application.bikeId) }
        verify(exactly = 0) { bikeDealer.order(any()) }
        verify(exactly = 0) { repository.save(any()) }
        confirmVerified(bikeDealer)
    }
}
