package io.miragon.blueprint.application.service;

import io.miragon.blueprint.application.port.inbound.RejectApplicationUseCase;
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository;
import io.miragon.blueprint.application.port.outbound.NotificationPort;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import io.miragon.blueprint.domain.leasing.LeasingApplication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RejectApplicationService implements RejectApplicationUseCase {

    private final LeasingApplicationRepository repository;
    private final NotificationPort notification;

    public RejectApplicationService(LeasingApplicationRepository repository, NotificationPort notification) {
        this.repository = repository;
        this.notification = notification;
    }

    @Override
    public void reject(ApplicationId id) {
        LeasingApplication application = repository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Unknown application " + id));
        notification.send("Your bike-leasing application was rejected", application);
        repository.save(application.reject());
    }
}
