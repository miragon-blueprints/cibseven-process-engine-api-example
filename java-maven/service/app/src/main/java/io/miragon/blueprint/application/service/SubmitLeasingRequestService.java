package io.miragon.blueprint.application.service;

import io.miragon.blueprint.application.port.inbound.SubmitLeasingRequestUseCase;
import io.miragon.blueprint.application.port.outbound.BikePortfolioRepository;
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository;
import io.miragon.blueprint.application.port.outbound.LeasingProcess;
import io.miragon.blueprint.domain.bike.Bike;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import io.miragon.blueprint.domain.leasing.LeasingApplication;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SubmitLeasingRequestService implements SubmitLeasingRequestUseCase {

    private final LeasingApplicationRepository repository;
    private final BikePortfolioRepository bikePortfolio;
    private final LeasingProcess process;
    private final Clock clock;

    public SubmitLeasingRequestService(
            LeasingApplicationRepository repository,
            BikePortfolioRepository bikePortfolio,
            LeasingProcess process,
            Clock clock) {
        this.repository = repository;
        this.bikePortfolio = bikePortfolio;
        this.process = process;
        this.clock = clock;
    }

    @Override
    public ApplicationId submit(SubmitLeasingRequestUseCase.Command command) {
        // The bike's model is kept in the portfolio; the application only references the bike by id.
        bikePortfolio.save(new Bike(command.bikeId(), command.bikeModel()));
        LeasingApplication application =
                LeasingApplication.receive(
                        ApplicationId.newId(),
                        command.customerName(),
                        command.email(),
                        command.age(),
                        command.monthlyNetIncome(),
                        command.bikeId(),
                        LocalDateTime.now(clock));
        repository.save(application);
        process.submitRequest(application);
        return application.id();
    }
}
