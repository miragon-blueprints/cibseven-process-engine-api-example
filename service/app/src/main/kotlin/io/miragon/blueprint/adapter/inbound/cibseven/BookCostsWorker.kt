package io.miragon.blueprint.adapter.inbound.cibseven

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker
import dev.bpmcrafters.processengine.worker.Variable
import io.miragon.blueprint.adapter.process.CancelBikeOrderProcessApi.ServiceTasks
import io.miragon.blueprint.application.port.inbound.BookCancellationCostsUseCase
import io.miragon.blueprint.domain.bike.OrderId
import org.springframework.stereotype.Component

/** Consumes the `bookCosts` external task of the `cancelBikeOrder` sub-process. */
@Component
class BookCostsWorker(
    private val useCase: BookCancellationCostsUseCase,
) {

    @ProcessEngineWorker(topic = ServiceTasks.BOOK_COSTS)
    fun bookCosts(@Variable orderId: String) {
        useCase.bookCosts(OrderId(orderId))
    }
}
