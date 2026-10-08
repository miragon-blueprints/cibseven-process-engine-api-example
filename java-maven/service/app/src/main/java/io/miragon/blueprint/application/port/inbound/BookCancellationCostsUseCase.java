package io.miragon.blueprint.application.port.inbound;

import io.miragon.blueprint.domain.bike.OrderId;

/** Books the costs incurred by cancelling a placed order. */
@FunctionalInterface
public interface BookCancellationCostsUseCase {

    void bookCosts(OrderId orderId);
}
