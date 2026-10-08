package io.miragon.blueprint.application.port.inbound

import io.miragon.blueprint.domain.bike.OrderId

/** Asks the dealer whether a placed order can still be cancelled. */
fun interface RequestOrderCancellationUseCase {
    fun requestCancellation(orderId: OrderId): Boolean
}
