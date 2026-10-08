package io.miragon.blueprint.application.service;

import io.miragon.blueprint.application.port.inbound.RequestOrderCancellationUseCase;
import io.miragon.blueprint.application.port.outbound.BikeDealerPort;
import io.miragon.blueprint.domain.bike.OrderId;
import org.springframework.stereotype.Service;

@Service
public class RequestOrderCancellationService implements RequestOrderCancellationUseCase {

    private final BikeDealerPort bikeDealer;

    public RequestOrderCancellationService(BikeDealerPort bikeDealer) {
        this.bikeDealer = bikeDealer;
    }

    @Override
    public boolean requestCancellation(OrderId orderId) {
        return bikeDealer.requestCancellation(orderId);
    }
}
