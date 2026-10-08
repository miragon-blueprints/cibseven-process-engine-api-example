package io.miragon.blueprint.application.service;

import io.miragon.blueprint.application.port.inbound.ActivateLeasingUseCase;
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import io.miragon.blueprint.domain.leasing.LeasingApplication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ActivateLeasingService implements ActivateLeasingUseCase {

    private final LeasingApplicationRepository repository;

    public ActivateLeasingService(LeasingApplicationRepository repository) {
        this.repository = repository;
    }

    @Override
    public void activate(ApplicationId id) {
        LeasingApplication application = repository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Unknown application " + id));
        repository.save(application.activate());
    }
}
