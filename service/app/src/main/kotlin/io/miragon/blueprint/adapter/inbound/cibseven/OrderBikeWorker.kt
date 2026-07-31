package io.miragon.blueprint.adapter.inbound.cibseven

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker
import dev.bpmcrafters.processengine.worker.Variable
import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.ServiceTasks
import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.Variables
import io.miragon.blueprint.application.port.inbound.OrderBikeUseCase
import io.miragon.blueprint.domain.leasing.ApplicationId
import org.springframework.stereotype.Component

/**
 * Consumes the `orderBike` external task and returns the order outcome as process variables
 * (`orderId`, `bikeAvailable`), which the following gateway routes on.
 */
@Component
class OrderBikeWorker(
    private val useCase: OrderBikeUseCase,
) {

    @ProcessEngineWorker(topic = ServiceTasks.ORDER_BIKE)
    fun orderBike(@Variable applicationId: String): Map<String, Any?> {
        val result = useCase.orderBike(ApplicationId.of(applicationId))
        return mapOf(
            Variables.ServiceTaskOrderBike.ORDER_ID.value to result.orderId?.value,
            Variables.ServiceTaskOrderBike.BIKE_AVAILABLE.value to result.bikeAvailable,
        )
    }
}
