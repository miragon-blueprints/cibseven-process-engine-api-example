package io.miragon.blueprint.application.port.inbound

import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.bike.OrderId

interface OrderBikeUseCase {
    fun orderBike(id: ApplicationId): Result

    data class Result(
        // null when the requested bike was out of stock and no order was placed
        val orderId: OrderId?,
        val bikeAvailable: Boolean,
    )
}
