package io.miragon.blueprint.application.service;

import io.miragon.blueprint.application.port.inbound.OrderBikeUseCase;
import io.miragon.blueprint.application.port.outbound.BikeDealerPort;
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository;
import io.miragon.blueprint.domain.bike.BikeId;
import io.miragon.blueprint.domain.bike.BikeUnavailableException;
import io.miragon.blueprint.domain.bike.OrderId;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import io.miragon.blueprint.domain.leasing.LeasingApplication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(noRollbackFor = BikeUnavailableException.class)
public class OrderBikeService implements OrderBikeUseCase {

    private final LeasingApplicationRepository repository;
    private final BikeDealerPort bikeDealer;

    public OrderBikeService(LeasingApplicationRepository repository, BikeDealerPort bikeDealer) {
        this.repository = repository;
        this.bikeDealer = bikeDealer;
    }

    @Override
    public OrderId orderBike(ApplicationId id, BikeId bikeId) {
        LeasingApplication storedApplication = repository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Unknown application " + id));
        LeasingApplication application = storedApplication.selectAlternative(bikeId);
        if (!bikeDealer.checkAvailability(bikeId)) {
            repository.save(application);
            throw new BikeUnavailableException(bikeId);
        }
        OrderId orderId = bikeDealer.order(bikeId);
        repository.save(application.documentOrder(orderId));
        return orderId;
    }
}
