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
 * scenario data: `BIKE-OOS` is the out-of-stock bike the `05-bike-unavailable` collection submits,
 * and `BIKE-FAIL` is the poison id the `06-incident-demo` collection uses to make the dealer "outage"
 * throw — see [order].
 */
@Component
class BikeDealerAdapter : BikeDealerPort {

    private val log = KotlinLogging.logger {}

    private val outOfStockBikeIds = setOf("BIKE-OOS")

    /**
     * Demo mechanism (not a real integration fault): ordering one of these bikes makes the dealer
     * call fail as if the dealer's API were unreachable. `serviceTask_orderBike` is an external task
     * consumed by [io.miragon.blueprint.adapter.inbound.cibseven.OrderBikeWorker], so an uncaught
     * failure fails that external task; the cib-seven-embedded adapter retries it (3 attempts, its
     * default retry policy) and then raises an incident — the basis of the reproducible incident
     * demo. It stays available in [checkAvailability] so the flow reaches the order step (rather than
     * the out-of-stock branch) before failing.
     */
    private val unavailableDealerBikeIds = setOf("BIKE-FAIL")

    override fun checkAvailability(bikeId: BikeId): Boolean {
        val available = bikeId.value !in outOfStockBikeIds
        return available
    }

    override fun order(bikeId: BikeId): OrderId {
        if (bikeId.value in unavailableDealerBikeIds) throw DealerUnavailableException(bikeId)
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

    /**
     * Signals that the (simulated) bike dealer could not be reached while placing an order. Thrown for
     * the `BIKE-FAIL` poison id to drive the reproducible incident demo: because `serviceTask_orderBike`
     * is an external task, this surfaces as a failed external task that retries and then raises an
     * incident. Nested so it stays colocated with the demo logic without needing an outbound-adapter
     * naming suffix.
     */
    class DealerUnavailableException(bikeId: BikeId) :
        RuntimeException("Bike dealer API unavailable for ${bikeId.value} (simulated demo outage)")
}
