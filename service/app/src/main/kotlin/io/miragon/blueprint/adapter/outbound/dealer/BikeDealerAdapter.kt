package io.miragon.blueprint.adapter.outbound.dealer

import io.miragon.blueprint.application.port.outbound.BikeDealerPort
import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.bike.OrderId
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Simulated bike dealer. Availability is decided by a small deny-list of bike ids so the
 * bike-unavailable → alternative branch can be triggered deterministically. The ids mirror the Bruno
 * scenario data: `BIKE-OOS` is the out-of-stock bike the `05-bike-unavailable` collection submits.
 */
@Component
class BikeDealerAdapter : BikeDealerPort {

    private val log = KotlinLogging.logger {}

    private val outOfStockBikeIds = setOf("BIKE-OOS")

    override fun checkAvailability(bikeId: BikeId): Boolean {
        val available = bikeId.value !in outOfStockBikeIds
        return available
    }

    override fun order(bikeId: BikeId): OrderId {
        val orderId = OrderId("ORDER-${UUID.randomUUID()}")
        log.info { "Placed order ${orderId.value} for bike ${bikeId.value}" }
        return orderId
    }

    override fun requestCancellation(orderId: OrderId): Boolean {
        // Demo dealer response: cancellation is always possible in this blueprint.
        log.info { "Requesting cancellation of order ${orderId.value}" }
        return true
    }

    override fun bookCancellationCosts(orderId: OrderId) {
        // A real system would book the dealer's cancellation fee here.
        log.info { "Booking cancellation costs for order ${orderId.value}" }
    }
}
