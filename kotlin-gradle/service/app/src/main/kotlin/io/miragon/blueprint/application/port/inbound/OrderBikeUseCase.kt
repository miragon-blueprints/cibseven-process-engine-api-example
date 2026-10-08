package io.miragon.blueprint.application.port.inbound

import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.bike.OrderId

interface OrderBikeUseCase {
    fun orderBike(id: ApplicationId): OrderId
}
