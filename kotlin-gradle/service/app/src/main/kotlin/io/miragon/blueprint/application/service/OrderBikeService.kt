package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.inbound.OrderBikeUseCase
import io.miragon.blueprint.application.port.outbound.BikeDealerPort
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.bike.BikeUnavailableException
import io.miragon.blueprint.domain.bike.OrderId
import io.miragon.blueprint.domain.leasing.ApplicationId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(noRollbackFor = [BikeUnavailableException::class])
class OrderBikeService(
    private val repository: LeasingApplicationRepository,
    private val bikeDealer: BikeDealerPort,
) : OrderBikeUseCase {

    override fun orderBike(id: ApplicationId, bikeId: BikeId): OrderId {
        val storedApplication = repository.findById(id) ?: error("Unknown application $id")
        val application = storedApplication.selectAlternative(bikeId)
        if (!bikeDealer.checkAvailability(bikeId)) {
            repository.save(application)
            throw BikeUnavailableException(bikeId)
        }
        val orderId = bikeDealer.order(bikeId)
        repository.save(application.documentOrder(orderId))
        return orderId
    }
}
