package io.miragon.blueprint.adapter.inbound.cibseven

import dev.bpmcrafters.processengine.worker.BpmnErrorOccurred
import dev.bpmcrafters.processengine.worker.ProcessEngineWorker
import dev.bpmcrafters.processengine.worker.Variable
import io.miragon.blueprint.adapter.process.Errors
import io.miragon.blueprint.adapter.process.ServiceTasks
import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.FlowNodes
import io.miragon.blueprint.application.port.inbound.OrderBikeUseCase
import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.bike.BikeUnavailableException
import io.miragon.blueprint.domain.leasing.ApplicationId
import org.springframework.stereotype.Component

/**
 * Consumes the `orderBike` external task and returns the placed order as the process variable
 * `orderId`, which the order compensation later reuses. An out-of-stock bike is reported to the engine
 * as the BPMN error `bikeUnavailable`, which the order task's boundary event catches — leaving the
 * task this way registers no order compensation.
 */
@Component
class OrderBikeWorker(
    private val useCase: OrderBikeUseCase,
) {

    @ProcessEngineWorker(topic = ServiceTasks.ORDER_BIKE)
    fun orderBike(@Variable applicationId: String, @Variable bikeId: String): Map<String, Any?> {
        val orderId = try {
            useCase.orderBike(ApplicationId.of(applicationId), BikeId(bikeId))
        } catch (e: BikeUnavailableException) {
            throw BpmnErrorOccurred(e.message.orEmpty(), Errors.BIKE_UNAVAILABLE.code, emptyMap())
        }
        return mapOf(FlowNodes.ServiceTaskOrderBike.Variables.ORDER_ID.value to orderId.value)
    }
}
