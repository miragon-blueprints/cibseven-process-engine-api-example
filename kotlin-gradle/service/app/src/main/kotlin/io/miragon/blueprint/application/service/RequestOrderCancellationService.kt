package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.inbound.RequestOrderCancellationUseCase
import io.miragon.blueprint.application.port.outbound.BikeDealerPort
import io.miragon.blueprint.domain.bike.OrderId
import org.springframework.stereotype.Service

@Service
class RequestOrderCancellationService(
    private val bikeDealer: BikeDealerPort,
) : RequestOrderCancellationUseCase {

    override fun requestCancellation(orderId: OrderId): Boolean = bikeDealer.requestCancellation(orderId)
}
