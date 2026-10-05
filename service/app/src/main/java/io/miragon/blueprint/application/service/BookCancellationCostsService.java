package io.miragon.blueprint.application.service;

import io.miragon.blueprint.application.port.inbound.BookCancellationCostsUseCase;
import io.miragon.blueprint.application.port.outbound.BikeDealerPort;
import io.miragon.blueprint.domain.bike.OrderId;
import org.springframework.stereotype.Service;

@Service
public class BookCancellationCostsService implements BookCancellationCostsUseCase {

    private final BikeDealerPort bikeDealer;

    public BookCancellationCostsService(BikeDealerPort bikeDealer) {
        this.bikeDealer = bikeDealer;
    }

    @Override
    public void bookCosts(OrderId orderId) {
        bikeDealer.bookCancellationCosts(orderId);
    }
}
