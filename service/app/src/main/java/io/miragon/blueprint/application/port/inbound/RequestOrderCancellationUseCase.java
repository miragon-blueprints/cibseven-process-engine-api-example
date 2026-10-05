package io.miragon.blueprint.application.port.inbound;

import io.miragon.blueprint.domain.bike.OrderId;

/** Asks the dealer whether a placed order can still be cancelled. */
@FunctionalInterface
public interface RequestOrderCancellationUseCase {

    boolean requestCancellation(OrderId orderId);
}
