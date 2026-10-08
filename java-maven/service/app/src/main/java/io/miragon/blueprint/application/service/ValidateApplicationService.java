package io.miragon.blueprint.application.service;

import io.miragon.blueprint.application.port.inbound.ValidateApplicationUseCase;
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import io.miragon.blueprint.domain.leasing.LeasingApplication;
import org.springframework.stereotype.Service;

@Service
public class ValidateApplicationService implements ValidateApplicationUseCase {

    private final LeasingApplicationRepository repository;

    public ValidateApplicationService(LeasingApplicationRepository repository) {
        this.repository = repository;
    }

    @Override
    public void validate(ApplicationId id) {
        LeasingApplication application = repository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Unknown application " + id));
        application.validate();
    }
}
