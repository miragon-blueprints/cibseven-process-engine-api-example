package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.inbound.OrderBikeUseCase
import io.miragon.blueprint.application.port.outbound.BikeDealerPort
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.domain.leasing.ApplicationId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class OrderBikeService(
    private val repository: LeasingApplicationRepository,
    private val bikeDealer: BikeDealerPort,
) : OrderBikeUseCase {

    override fun orderBike(id: ApplicationId): OrderBikeUseCase.Result {
        val application = repository.findById(id) ?: error("Unknown application $id")
        if (!bikeDealer.checkAvailability(application.bikeId)) {
            return OrderBikeUseCase.Result(orderId = null, bikeAvailable = false)
        } else {
            val orderId = bikeDealer.order(application.bikeId)
            repository.save(application.documentOrder(orderId))
            return OrderBikeUseCase.Result(orderId, bikeAvailable = true)   
        }
    }
}
