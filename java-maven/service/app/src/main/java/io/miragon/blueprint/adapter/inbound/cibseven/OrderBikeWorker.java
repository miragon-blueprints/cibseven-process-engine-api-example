package io.miragon.blueprint.adapter.inbound.cibseven;

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker;
import dev.bpmcrafters.processengine.worker.Variable;
import io.miragon.blueprint.adapter.process.ServiceTasks;
import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.FlowNodes;
import io.miragon.blueprint.application.port.inbound.OrderBikeUseCase;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Consumes the {@code orderBike} external task and returns the order outcome as process variables
 * ({@code orderId}, {@code bikeAvailable}), which the following gateway routes on.
 */
@Component
public class OrderBikeWorker {

    private final OrderBikeUseCase useCase;

    public OrderBikeWorker(OrderBikeUseCase useCase) {
        this.useCase = useCase;
    }

    @ProcessEngineWorker(topic = ServiceTasks.ORDER_BIKE)
    public Map<String, Object> orderBike(@Variable String applicationId) {
        OrderBikeUseCase.Result result = useCase.orderBike(ApplicationId.of(applicationId));
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put(FlowNodes.ServiceTaskOrderBike.Variables.ORDER_ID.getValue(),
                result.orderId() == null ? null : result.orderId().value());
        variables.put(FlowNodes.ServiceTaskOrderBike.Variables.BIKE_AVAILABLE.getValue(), result.bikeAvailable());
        return variables;
    }
}
