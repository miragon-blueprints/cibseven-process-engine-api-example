package io.miragon.blueprint.application.service;

import static io.miragon.blueprint.domain.leasing.TestObjectBuilder.testLeasingApplication;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.miragon.blueprint.application.port.outbound.BikeDealerPort;
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository;
import io.miragon.blueprint.domain.bike.BikeId;
import io.miragon.blueprint.domain.bike.BikeUnavailableException;
import io.miragon.blueprint.domain.bike.OrderId;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import io.miragon.blueprint.domain.leasing.LeasingApplication;
import io.miragon.blueprint.domain.leasing.LeasingStatus;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderBikeServiceTest {

    private final LeasingApplicationRepository repository = mock(LeasingApplicationRepository.class);
    private final BikeDealerPort bikeDealer = mock(BikeDealerPort.class);
    private final OrderBikeService underTest = new OrderBikeService(repository, bikeDealer);

    @Test
    void orderBike_places_an_order_when_the_dealer_has_the_bike_in_stock() {

        // given: an application whose bike is available at the dealer
        LeasingApplication application = testLeasingApplication().bikeId(new BikeId("BIKE-900")).build();
        when(repository.findById(application.id())).thenReturn(Optional.of(application));
        when(bikeDealer.checkAvailability(application.bikeId())).thenReturn(true);
        when(bikeDealer.order(application.bikeId())).thenReturn(new OrderId("ORDER-900"));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // when: the bike is ordered
        OrderId orderId = underTest.orderBike(application.id(), application.bikeId());

        // then: the order id is returned and the application moves to ORDERED
        assertThat(orderId).isEqualTo(new OrderId("ORDER-900"));
        verify(bikeDealer).checkAvailability(application.bikeId());
        verify(bikeDealer).order(application.bikeId());
        verify(repository).save(argThat(saved ->
                saved.status() == LeasingStatus.ORDERED && new OrderId("ORDER-900").equals(saved.orderId())));
        verify(repository).findById(application.id());
        verifyNoMoreInteractions(bikeDealer, repository);
    }

    @Test
    void orderBike_orders_the_bike_the_process_carries_and_stores_it_on_the_application() {

        // given: an application still pointing at the bike that was requested first
        LeasingApplication application = testLeasingApplication().bikeId(new BikeId("BIKE-OOS")).build();
        BikeId alternative = new BikeId("BIKE-ALT");
        when(repository.findById(application.id())).thenReturn(Optional.of(application));
        when(bikeDealer.checkAvailability(alternative)).thenReturn(true);
        when(bikeDealer.order(alternative)).thenReturn(new OrderId("ORDER-ALT"));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // when: the process asks for the alternative bike
        OrderId orderId = underTest.orderBike(application.id(), alternative);

        // then: the alternative is checked, ordered and stored together with the order
        assertThat(orderId).isEqualTo(new OrderId("ORDER-ALT"));
        verify(bikeDealer).checkAvailability(alternative);
        verify(bikeDealer).order(alternative);
        verify(repository).save(argThat(saved ->
                saved.bikeId().equals(alternative) && new OrderId("ORDER-ALT").equals(saved.orderId())));
        verify(repository).findById(application.id());
        verifyNoMoreInteractions(bikeDealer, repository);
    }

    @Test
    void orderBike_reports_an_out_of_stock_bike_as_unavailable_and_places_no_order() {

        // given: an application whose bike is out of stock at the dealer
        LeasingApplication application = testLeasingApplication().bikeId(new BikeId("BIKE-OOS")).build();
        when(repository.findById(application.id())).thenReturn(Optional.of(application));
        when(bikeDealer.checkAvailability(application.bikeId())).thenReturn(false);

        // when / then: ordering reports the bike as unavailable and places no order
        assertThatThrownBy(() -> underTest.orderBike(application.id(), application.bikeId()))
                .isInstanceOf(BikeUnavailableException.class)
                .hasMessage("Bike BIKE-OOS is not available at the dealer");
        verify(bikeDealer).checkAvailability(application.bikeId());
        verify(bikeDealer, never()).order(any());
        verify(repository, never()).save(argThat(saved -> saved.orderId() != null));
        verifyNoMoreInteractions(bikeDealer);
    }

    @Test
    void orderBike_keeps_an_unavailable_alternative_on_the_application() {

        // given: an application whose alternative is out of stock as well
        LeasingApplication application = testLeasingApplication().bikeId(new BikeId("BIKE-900")).build();
        BikeId alternative = new BikeId("BIKE-OOS");
        when(repository.findById(application.id())).thenReturn(Optional.of(application));
        when(bikeDealer.checkAvailability(alternative)).thenReturn(false);

        // when / then: the alternative is reported as unavailable, yet the application points at it
        assertThatThrownBy(() -> underTest.orderBike(application.id(), alternative))
                .isInstanceOf(BikeUnavailableException.class)
                .hasMessage("Bike BIKE-OOS is not available at the dealer");
        verify(repository).save(argThat(saved -> saved.bikeId().equals(alternative) && saved.orderId() == null));
        verify(bikeDealer, never()).order(any());
    }

    @Test
    void orderBike_fails_for_an_unknown_application() {

        // given: an id the repository cannot resolve
        ApplicationId unknownId = new ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174999"));
        when(repository.findById(unknownId)).thenReturn(Optional.empty());

        // when / then: ordering fails with the unknown-application message, the dealer is never asked
        assertThatThrownBy(() -> underTest.orderBike(unknownId, new BikeId("BIKE-900")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unknown application " + unknownId);
        verify(repository).findById(unknownId);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(bikeDealer);
    }
}
