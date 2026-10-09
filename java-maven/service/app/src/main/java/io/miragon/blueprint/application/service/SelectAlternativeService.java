package io.miragon.blueprint.application.service;

import io.miragon.blueprint.application.port.inbound.SelectAlternativeUseCase;
import io.miragon.blueprint.application.port.outbound.BikePortfolioRepository;
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository;
import io.miragon.blueprint.application.port.outbound.LeasingProcess;
import io.miragon.blueprint.domain.bike.Bike;
import io.miragon.blueprint.domain.bike.BikeId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SelectAlternativeService implements SelectAlternativeUseCase {

    private final LeasingApplicationRepository repository;
    private final BikePortfolioRepository bikePortfolio;
    private final LeasingProcess process;

    public SelectAlternativeService(
            LeasingApplicationRepository repository,
            BikePortfolioRepository bikePortfolio,
            LeasingProcess process) {
        this.repository = repository;
        this.bikePortfolio = bikePortfolio;
        this.process = process;
    }

    @Override
    public void selectAlternative(SelectAlternativeUseCase.Command command) {
        repository.findById(command.applicationId())
                .orElseThrow(() -> new IllegalStateException("Unknown application " + command.applicationId()));
        BikeId alternativeBike = command.bikeId();
        if (command.alternativeFound() && alternativeBike != null && command.bikeModel() != null) {
            bikePortfolio.save(new Bike(alternativeBike, command.bikeModel()));
        }
        process.completeAlternativeClarification(
                command.applicationId(),
                command.alternativeFound(),
                command.bikeId());
    }
}
