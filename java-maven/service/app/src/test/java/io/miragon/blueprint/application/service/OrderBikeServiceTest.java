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

import io.miragon.blueprint.application.port.inbound.OrderBikeUseCase;
import io.miragon.blueprint.application.port.outbound.BikeDealerPort;
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository;
import io.miragon.blueprint.domain.bike.BikeId;
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
        OrderBikeUseCase.Result result = underTest.orderBike(application.id());

        // then: the order id is returned and the application moves to ORDERED
        assertThat(result.bikeAvailable()).isTrue();
        assertThat(result.orderId()).isEqualTo(new OrderId("ORDER-900"));
        verify(bikeDealer).checkAvailability(application.bikeId());
        verify(bikeDealer).order(application.bikeId());
        verify(repository).save(argThat(saved ->
                saved.status() == LeasingStatus.ORDERED && new OrderId("ORDER-900").equals(saved.orderId())));
        verify(repository).findById(application.id());
        verifyNoMoreInteractions(bikeDealer, repository);
    }

    @Test
    void orderBike_reports_an_out_of_stock_bike_as_unavailable_and_places_no_order() {

        // given: an application whose bike is out of stock at the dealer
        LeasingApplication application = testLeasingApplication().bikeId(new BikeId("BIKE-OOS")).build();
        when(repository.findById(application.id())).thenReturn(Optional.of(application));
        when(bikeDealer.checkAvailability(application.bikeId())).thenReturn(false);

        // when: the bike is ordered
        OrderBikeUseCase.Result result = underTest.orderBike(application.id());

        // then: no order is placed and the bike is reported unavailable
        assertThat(result.bikeAvailable()).isFalse();
        assertThat(result.orderId()).isNull();
        verify(bikeDealer).checkAvailability(application.bikeId());
        verify(bikeDealer, never()).order(any());
        verify(repository, never()).save(any());
        verify(repository).findById(application.id());
        verifyNoMoreInteractions(bikeDealer, repository);
    }

    @Test
    void orderBike_fails_for_an_unknown_application() {

        // given: an id the repository cannot resolve
        ApplicationId unknownId = new ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174999"));
        when(repository.findById(unknownId)).thenReturn(Optional.empty());

        // when / then: ordering fails with the unknown-application message, the dealer is never asked
        assertThatThrownBy(() -> underTest.orderBike(unknownId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unknown application " + unknownId);
        verify(repository).findById(unknownId);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(bikeDealer);
    }
}
