package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.outbound.BikeDealerPort
import io.miragon.blueprint.domain.bike.OrderId
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RequestOrderCancellationServiceTest {

    private val bikeDealer = mockk<BikeDealerPort>()
    private val underTest = RequestOrderCancellationService(bikeDealer = bikeDealer)

    @Test
    fun `requestCancellation asks the dealer and returns its answer`() {
        // given: a placed order the dealer allows cancelling
        val orderId = OrderId("ORDER-900")
        every { bikeDealer.requestCancellation(orderId) } returns true
        // when: the dealer is asked whether it can be cancelled
        val possible = underTest.requestCancellation(orderId)
        // then: the dealer's answer is returned
        assertThat(possible).isTrue()
        verify { bikeDealer.requestCancellation(orderId) }
        confirmVerified(bikeDealer)
    }
}
