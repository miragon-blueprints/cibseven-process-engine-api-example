package io.miragon.blueprint.adapter.outbound.dealer;

import io.miragon.blueprint.application.port.outbound.BikeDealerPort;
import io.miragon.blueprint.domain.bike.BikeId;
import io.miragon.blueprint.domain.bike.OrderId;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Simulated bike dealer. Availability is decided by a small deny-list of bike ids so the
 * bike-unavailable → alternative branch can be triggered deterministically. The ids mirror the Bruno
 * scenario data: {@code BIKE-OOS} is the out-of-stock bike the {@code 05-bike-unavailable} collection
 * submits, and {@code BIKE-FAIL} is the poison id the {@code 06-incident-demo} collection uses to make the
 * dealer "outage" throw — see {@link #order}.
 */
@Component
public class BikeDealerAdapter implements BikeDealerPort {

    private static final Logger log = LoggerFactory.getLogger(BikeDealerAdapter.class);

    private static final Set<String> OUT_OF_STOCK_BIKE_IDS = Set.of("BIKE-OOS");

    /**
     * Demo mechanism (not a real integration fault): ordering one of these bikes makes the dealer
     * call fail as if the dealer's API were unreachable. {@code serviceTask_orderBike} is an external task
     * consumed by {@link io.miragon.blueprint.adapter.inbound.cibseven.OrderBikeWorker}, so an uncaught
     * failure fails that external task; the cib-seven-embedded adapter retries it (3 attempts, its
     * default retry policy) and then raises an incident — the basis of the reproducible incident
     * demo. It stays available in {@link #checkAvailability} so the flow reaches the order step (rather than
     * the out-of-stock branch) before failing.
     */
    private static final Set<String> UNAVAILABLE_DEALER_BIKE_IDS = Set.of("BIKE-FAIL");

    @Override
    public boolean checkAvailability(BikeId bikeId) {
        return !OUT_OF_STOCK_BIKE_IDS.contains(bikeId.value());
    }

    @Override
    public OrderId order(BikeId bikeId) {
        if (UNAVAILABLE_DEALER_BIKE_IDS.contains(bikeId.value())) {
            throw new DealerUnavailableException(bikeId);
        }
        OrderId orderId = new OrderId("ORDER-" + UUID.randomUUID());
        log.info("Placed order {} for bike {}", orderId.value(), bikeId.value());
        return orderId;
    }

    @Override
    public boolean requestCancellation(OrderId orderId) {
        // Demo dealer response: cancellation is always possible in this blueprint.
        log.info("Requesting cancellation of order {}", orderId.value());
        return true;
    }

    @Override
    public void bookCancellationCosts(OrderId orderId) {
        // A real system would book the dealer's cancellation fee here.
        log.info("Booking cancellation costs for order {}", orderId.value());
    }

    /**
     * Signals that the (simulated) bike dealer could not be reached while placing an order. Thrown for
     * the {@code BIKE-FAIL} poison id to drive the reproducible incident demo: because
     * {@code serviceTask_orderBike} is an external task, this surfaces as a failed external task that retries
     * and then raises an incident. Nested so it stays colocated with the demo logic without needing an
     * outbound-adapter naming suffix.
     */
    public static final class DealerUnavailableException extends RuntimeException {

        public DealerUnavailableException(BikeId bikeId) {
            super("Bike dealer API unavailable for " + bikeId.value() + " (simulated demo outage)");
        }
    }
}
