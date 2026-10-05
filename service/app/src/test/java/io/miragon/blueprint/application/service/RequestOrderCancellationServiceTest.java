package io.miragon.blueprint.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.miragon.blueprint.application.port.outbound.BikeDealerPort;
import io.miragon.blueprint.domain.bike.OrderId;
import org.junit.jupiter.api.Test;

class RequestOrderCancellationServiceTest {

    private final BikeDealerPort bikeDealer = mock(BikeDealerPort.class);
    private final RequestOrderCancellationService underTest = new RequestOrderCancellationService(bikeDealer);

    @Test
    void requestCancellation_asks_the_dealer_and_returns_its_answer() {
        // given: a placed order the dealer allows cancelling
        OrderId orderId = new OrderId("ORDER-900");
        when(bikeDealer.requestCancellation(orderId)).thenReturn(true);
        // when: the dealer is asked whether it can be cancelled
        boolean possible = underTest.requestCancellation(orderId);
        // then: the dealer's answer is returned
        assertThat(possible).isTrue();
        verify(bikeDealer).requestCancellation(orderId);
        verifyNoMoreInteractions(bikeDealer);
    }

    @Test
    void requestCancellation_returns_false_when_the_dealer_refuses() {
        // given: an order the dealer will not allow cancelling
        OrderId orderId = new OrderId("ORDER-901");
        when(bikeDealer.requestCancellation(orderId)).thenReturn(false);
        // when: the dealer is asked whether it can be cancelled
        boolean possible = underTest.requestCancellation(orderId);
        // then: the dealer's negative answer is propagated unchanged
        assertThat(possible).isFalse();
        verify(bikeDealer).requestCancellation(orderId);
        verifyNoMoreInteractions(bikeDealer);
    }
}
