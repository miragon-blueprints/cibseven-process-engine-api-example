package io.miragon.blueprint.adapter.inbound.cibseven;

import dev.bpmcrafters.processengine.worker.BpmnErrorOccurred;
import dev.bpmcrafters.processengine.worker.ProcessEngineWorker;
import dev.bpmcrafters.processengine.worker.Variable;
import io.miragon.blueprint.adapter.process.Errors;
import io.miragon.blueprint.adapter.process.ServiceTasks;
import io.miragon.blueprint.adapter.process.BikeLeasingProcessProcessApi.FlowNodes;
import io.miragon.blueprint.application.port.inbound.OrderBikeUseCase;
import io.miragon.blueprint.domain.bike.BikeUnavailableException;
import io.miragon.blueprint.domain.bike.OrderId;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Consumes the {@code orderBike} external task and returns the placed order as the process variable
 * {@code orderId}, which the order compensation later reuses. An out-of-stock bike is reported to the
 * engine as the BPMN error {@code bikeUnavailable}, which the order task's boundary event catches —
 * leaving the task this way registers no order compensation.
 */
@Component
public class OrderBikeWorker {

    private final OrderBikeUseCase useCase;

    public OrderBikeWorker(OrderBikeUseCase useCase) {
        this.useCase = useCase;
    }

    @ProcessEngineWorker(topic = ServiceTasks.ORDER_BIKE)
    public Map<String, Object> orderBike(@Variable String applicationId) throws BpmnErrorOccurred {
        try {
            OrderId orderId = useCase.orderBike(ApplicationId.of(applicationId));
            return Map.of(FlowNodes.ServiceTaskOrderBike.Variables.ORDER_ID.getValue(), orderId.value());
        } catch (BikeUnavailableException e) {
            throw new BpmnErrorOccurred(e.getMessage(), Errors.BIKE_UNAVAILABLE.getCode(), Map.of());
        }
    }
}
