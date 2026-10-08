package io.miragon.blueprint.application.service;

import io.miragon.blueprint.application.port.inbound.OrderBikeUseCase;
import io.miragon.blueprint.application.port.outbound.BikeDealerPort;
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository;
import io.miragon.blueprint.domain.bike.OrderId;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import io.miragon.blueprint.domain.leasing.LeasingApplication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderBikeService implements OrderBikeUseCase {

    private final LeasingApplicationRepository repository;
    private final BikeDealerPort bikeDealer;

    public OrderBikeService(LeasingApplicationRepository repository, BikeDealerPort bikeDealer) {
        this.repository = repository;
        this.bikeDealer = bikeDealer;
    }

    @Override
    public OrderBikeUseCase.Result orderBike(ApplicationId id) {
        LeasingApplication application = repository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Unknown application " + id));
        if (!bikeDealer.checkAvailability(application.bikeId())) {
            return new OrderBikeUseCase.Result(null, false);
        } else {
            OrderId orderId = bikeDealer.order(application.bikeId());
            repository.save(application.documentOrder(orderId));
            return new OrderBikeUseCase.Result(orderId, true);
        }
    }
}
