package io.miragon.blueprint.adapter.inbound.cibseven;

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker;
import dev.bpmcrafters.processengine.worker.Variable;
import io.miragon.blueprint.adapter.process.CancelBikeOrderProcessApi.ServiceTasks;
import io.miragon.blueprint.application.port.inbound.BookCancellationCostsUseCase;
import io.miragon.blueprint.domain.bike.OrderId;
import org.springframework.stereotype.Component;

/** Consumes the {@code bookCosts} external task of the {@code cancelBikeOrder} sub-process. */
@Component
public class BookCostsWorker {

    private final BookCancellationCostsUseCase useCase;

    public BookCostsWorker(BookCancellationCostsUseCase useCase) {
        this.useCase = useCase;
    }

    @ProcessEngineWorker(topic = ServiceTasks.BOOK_COSTS)
    public void bookCosts(@Variable String orderId) {
        useCase.bookCosts(new OrderId(orderId));
    }
}
