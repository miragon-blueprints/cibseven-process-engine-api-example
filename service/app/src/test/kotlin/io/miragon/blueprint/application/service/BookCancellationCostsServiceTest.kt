package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.outbound.BikeDealerPort
import io.miragon.blueprint.domain.bike.OrderId
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class BookCancellationCostsServiceTest {

    private val bikeDealer = mockk<BikeDealerPort>()
    private val underTest = BookCancellationCostsService(bikeDealer = bikeDealer)

    @Test
    fun `bookCosts delegates the cost booking to the dealer`() {
        // given: a placed order
        val orderId = OrderId("ORDER-900")
        every { bikeDealer.bookCancellationCosts(orderId) } just Runs
        // when: the costs are booked
        underTest.bookCosts(orderId)
        // then: the dealer out-port books the cancellation costs
        verify { bikeDealer.bookCancellationCosts(orderId) }
        confirmVerified(bikeDealer)
    }
}
