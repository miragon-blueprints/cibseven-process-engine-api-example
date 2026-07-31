package io.miragon.blueprint.adapter.inbound.cibseven

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker
import dev.bpmcrafters.processengine.worker.Variable
import io.miragon.blueprint.adapter.process.CancelBikeOrderProcessApi.ServiceTasks
import io.miragon.blueprint.adapter.process.CancelBikeOrderProcessApi.Variables
import io.miragon.blueprint.application.port.inbound.RequestOrderCancellationUseCase
import io.miragon.blueprint.domain.bike.OrderId
import org.springframework.stereotype.Component

/**
 * Consumes the `requestCancellation` external task of the `cancelBikeOrder` sub-process. `orderId`
 * is handed in by the calling activity; the outcome (`cancellationPossible`) routes the gateway.
 */
@Component
class RequestCancellationWorker(
    private val useCase: RequestOrderCancellationUseCase,
) {

    @ProcessEngineWorker(topic = ServiceTasks.REQUEST_CANCELLATION)
    fun requestCancellation(@Variable orderId: String): Map<String, Any?> {
        val cancellationPossible = useCase.requestCancellation(OrderId(orderId))
        return mapOf(Variables.ServiceTaskRequestCancellation.CANCELLATION_POSSIBLE.value to cancellationPossible)
    }
}
