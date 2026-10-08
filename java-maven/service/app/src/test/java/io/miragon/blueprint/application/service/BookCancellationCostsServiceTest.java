package io.miragon.blueprint.application.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import io.miragon.blueprint.application.port.outbound.BikeDealerPort;
import io.miragon.blueprint.domain.bike.OrderId;
import org.junit.jupiter.api.Test;

class BookCancellationCostsServiceTest {

    private final BikeDealerPort bikeDealer = mock(BikeDealerPort.class);
    private final BookCancellationCostsService underTest = new BookCancellationCostsService(bikeDealer);

    @Test
    void bookCosts_delegates_the_cost_booking_to_the_dealer() {
        // given: a placed order
        OrderId orderId = new OrderId("ORDER-900");
        // when: the costs are booked
        underTest.bookCosts(orderId);
        // then: the dealer out-port books the cancellation costs
        verify(bikeDealer).bookCancellationCosts(orderId);
        verifyNoMoreInteractions(bikeDealer);
    }
}
