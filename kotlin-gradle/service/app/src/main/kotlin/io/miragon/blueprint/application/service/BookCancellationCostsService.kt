package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.inbound.BookCancellationCostsUseCase
import io.miragon.blueprint.application.port.outbound.BikeDealerPort
import io.miragon.blueprint.domain.bike.OrderId
import org.springframework.stereotype.Service

@Service
class BookCancellationCostsService(
    private val bikeDealer: BikeDealerPort,
) : BookCancellationCostsUseCase {

    override fun bookCosts(orderId: OrderId) = bikeDealer.bookCancellationCosts(orderId)
}
