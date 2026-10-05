package io.miragon.blueprint.application.port.outbound;

import io.miragon.blueprint.domain.bike.BikeId;
import io.miragon.blueprint.domain.bike.OrderId;

/**
 * Outbound port to the (simulated) bike dealer: it answers whether a bike is in stock, places the
 * order, and handles cancelling a placed order. Implemented by an infrastructure adapter, so the
 * dealer rules live outside the domain.
 */
public interface BikeDealerPort {

    boolean checkAvailability(BikeId bikeId);

    OrderId order(BikeId bikeId);

    /** Asks the dealer whether a placed order can still be cancelled. */
    boolean requestCancellation(OrderId orderId);

    /** Books the costs the dealer charges for cancelling a placed order. */
    void bookCancellationCosts(OrderId orderId);
}
