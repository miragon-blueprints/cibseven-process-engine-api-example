package io.miragon.blueprint.adapter.inbound.cibseven;

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker;
import dev.bpmcrafters.processengine.worker.Variable;
import io.miragon.blueprint.adapter.process.ServiceTasks;
import io.miragon.blueprint.adapter.process.CancelBikeOrderProcessApi.FlowNodes;
import io.miragon.blueprint.application.port.inbound.RequestOrderCancellationUseCase;
import io.miragon.blueprint.domain.bike.OrderId;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Consumes the {@code requestCancellation} external task of the {@code cancelBikeOrder} sub-process. {@code orderId}
 * is handed in by the calling activity; the outcome ({@code cancellationPossible}) routes the gateway.
 */
@Component
public class RequestCancellationWorker {

    private final RequestOrderCancellationUseCase useCase;

    public RequestCancellationWorker(RequestOrderCancellationUseCase useCase) {
        this.useCase = useCase;
    }

    @ProcessEngineWorker(topic = ServiceTasks.REQUEST_CANCELLATION)
    public Map<String, Object> requestCancellation(@Variable String orderId) {
        boolean cancellationPossible = useCase.requestCancellation(new OrderId(orderId));
        return Map.of(FlowNodes.ServiceTaskRequestCancellation.Variables.CANCELLATION_POSSIBLE.getValue(), cancellationPossible);
    }
}
