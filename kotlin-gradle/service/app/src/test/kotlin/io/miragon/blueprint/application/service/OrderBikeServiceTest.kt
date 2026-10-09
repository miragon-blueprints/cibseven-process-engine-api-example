package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.outbound.BikeDealerPort
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.bike.BikeUnavailableException
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.LeasingStatus
import io.miragon.blueprint.domain.bike.OrderId
import io.miragon.blueprint.domain.leasing.testLeasingApplication
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.UUID

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
        val orderId = underTest.orderBike(application.id, application.bikeId)

        // then: the order id is returned and the application moves to ORDERED
        assertThat(orderId).isEqualTo(OrderId("ORDER-900"))
        verify { bikeDealer.checkAvailability(application.bikeId) }
        verify { bikeDealer.order(application.bikeId) }
        verify { repository.save(match { it.status == LeasingStatus.ORDERED && it.orderId == OrderId("ORDER-900") }) }
        confirmVerified(bikeDealer)
    }

    @Test
    fun `orderBike orders the bike the process carries and stores it on the application`() {

        // given: an application still pointing at the bike that was requested first
        val application = testLeasingApplication(bikeId = BikeId("BIKE-OOS"))
        every { repository.findById(application.id) } returns application
        every { bikeDealer.checkAvailability(BikeId("BIKE-ALT")) } returns true
        every { bikeDealer.order(BikeId("BIKE-ALT")) } returns OrderId("ORDER-ALT")
        every { repository.save(any()) } answers { firstArg() }

        // when: the process asks for the alternative bike
        val orderId = underTest.orderBike(application.id, BikeId("BIKE-ALT"))

        // then: the alternative is checked, ordered and stored together with the order
        assertThat(orderId).isEqualTo(OrderId("ORDER-ALT"))
        verify { bikeDealer.checkAvailability(BikeId("BIKE-ALT")) }
        verify { bikeDealer.order(BikeId("BIKE-ALT")) }
        verify(exactly = 1) {
            repository.save(match { it.bikeId == BikeId("BIKE-ALT") && it.orderId == OrderId("ORDER-ALT") })
        }
        confirmVerified(bikeDealer)
    }

    @Test
    fun `orderBike reports an out-of-stock bike as unavailable and places no order`() {

        // given: an application whose bike is out of stock at the dealer
        val application = testLeasingApplication(bikeId = BikeId("BIKE-OOS"))
        every { repository.findById(application.id) } returns application
        every { bikeDealer.checkAvailability(application.bikeId) } returns false
        every { repository.save(any()) } answers { firstArg() }

        // when / then: ordering reports the bike as unavailable and places no order
        assertThatThrownBy { underTest.orderBike(application.id, application.bikeId) }
            .isInstanceOf(BikeUnavailableException::class.java)
            .hasMessage("Bike BIKE-OOS is not available at the dealer")
        verify { bikeDealer.checkAvailability(application.bikeId) }
        verify(exactly = 0) { bikeDealer.order(any()) }
        verify(exactly = 0) { repository.save(match { it.orderId != null }) }
        confirmVerified(bikeDealer)
    }

    @Test
    fun `orderBike keeps an unavailable alternative on the application`() {

        // given: an application whose alternative is out of stock as well
        val application = testLeasingApplication(bikeId = BikeId("BIKE-900"))
        every { repository.findById(application.id) } returns application
        every { bikeDealer.checkAvailability(BikeId("BIKE-OOS")) } returns false
        every { repository.save(any()) } answers { firstArg() }

        // when / then: the alternative is reported as unavailable, yet the application points at it
        assertThatThrownBy { underTest.orderBike(application.id, BikeId("BIKE-OOS")) }
            .isInstanceOf(BikeUnavailableException::class.java)
            .hasMessage("Bike BIKE-OOS is not available at the dealer")
        verify(exactly = 1) { repository.save(match { it.bikeId == BikeId("BIKE-OOS") && it.orderId == null }) }
        verify(exactly = 0) { bikeDealer.order(any()) }
    }

    @Test
    fun `orderBike fails for an unknown application`() {

        // given: an id the repository cannot resolve
        val unknownId = ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174999"))
        every { repository.findById(unknownId) } returns null

        // when / then: ordering fails with the unknown-application message, the dealer is never asked
        assertThatThrownBy { underTest.orderBike(unknownId, BikeId("BIKE-900")) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Unknown application $unknownId")
        verify { repository.findById(unknownId) }
        confirmVerified(repository, bikeDealer)
    }
}
