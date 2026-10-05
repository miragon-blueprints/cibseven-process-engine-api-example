package io.miragon.blueprint.adapter.outbound.dealer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.miragon.blueprint.domain.bike.BikeId;
import io.miragon.blueprint.domain.bike.OrderId;
import org.junit.jupiter.api.Test;

class BikeDealerAdapterTest {

    private final BikeDealerAdapter underTest = new BikeDealerAdapter();

    @Test
    void a_bike_outside_the_out_of_stock_list_is_available() {
        // given: a regularly stocked bike / when-then: the dealer reports it as available
        assertThat(underTest.checkAvailability(new BikeId("BIKE-900"))).isTrue();
    }

    @Test
    void the_out_of_stock_demo_bike_is_unavailable() {
        // given: the sentinel out-of-stock bike / when-then: the dealer reports it as unavailable
        assertThat(underTest.checkAvailability(new BikeId("BIKE-OOS"))).isFalse();
    }

    @Test
    void order_mints_a_fresh_unique_order_id() {
        // given: the same bike ordered twice
        OrderId first = underTest.order(new BikeId("BIKE-900"));
        OrderId second = underTest.order(new BikeId("BIKE-900"));
        // then: each order gets its own opaque reference, independent of the bike id
        assertThat(first.value()).startsWith("ORDER-");
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void the_incident_demo_bike_stays_available_so_the_flow_reaches_the_order_step() {
        // given: the poison bike used by the incident demo / when-then: it is not on the out-of-stock
        // list, so the process reaches serviceTask_orderBike (where the dealer then fails) instead of
        // taking the out-of-stock branch
        assertThat(underTest.checkAvailability(new BikeId("BIKE-FAIL"))).isTrue();
    }

    @Test
    void ordering_the_incident_demo_bike_fails_as_a_simulated_dealer_outage() {
        // given: the poison bike / when-then: the dealer call throws so the external task fails → incident
        assertThatThrownBy(() -> underTest.order(new BikeId("BIKE-FAIL")))
                .isInstanceOf(BikeDealerAdapter.DealerUnavailableException.class)
                .hasMessage("Bike dealer API unavailable for BIKE-FAIL (simulated demo outage)");
    }

    @Test
    void requestCancellation_reports_the_order_as_cancellable() {
        // given: a placed order / when-then: the demo dealer always allows cancellation
        assertThat(underTest.requestCancellation(new OrderId("ORDER-900"))).isTrue();
    }

    @Test
    void bookCancellationCosts_runs_without_error() {
        // given: a placed order / when-then: booking the dealer's costs runs without error
        assertThatCode(() -> underTest.bookCancellationCosts(new OrderId("ORDER-900"))).doesNotThrowAnyException();
    }
}
