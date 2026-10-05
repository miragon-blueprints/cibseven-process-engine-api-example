package io.miragon.blueprint.application.service;

import io.miragon.blueprint.application.port.inbound.GetLeasingApplicationQuery;
import io.miragon.blueprint.application.port.outbound.BikePortfolioRepository;
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository;
import io.miragon.blueprint.domain.bike.Bike;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetLeasingApplicationService implements GetLeasingApplicationQuery {

    private final LeasingApplicationRepository repository;
    private final BikePortfolioRepository bikePortfolio;

    public GetLeasingApplicationService(
            LeasingApplicationRepository repository,
            BikePortfolioRepository bikePortfolio) {
        this.repository = repository;
        this.bikePortfolio = bikePortfolio;
    }

    @Override
    public Optional<GetLeasingApplicationQuery.Result> byId(ApplicationId id) {
        return repository.findById(id).map(application -> {
            Optional<Bike> bike = bikePortfolio.findByBikeId(application.bikeId());
            return new GetLeasingApplicationQuery.Result(application, bike.map(Bike::model).orElse(null));
        });
    }
}
