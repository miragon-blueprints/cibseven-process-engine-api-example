package io.miragon.blueprint.application.service;

import io.miragon.blueprint.application.port.inbound.SendCancellationConfirmationUseCase;
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository;
import io.miragon.blueprint.application.port.outbound.NotificationPort;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import io.miragon.blueprint.domain.leasing.LeasingApplication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Final step of the cancellation path (serviceTask_sendCancellationConfirmation →
 * endEvent_applicationCancelled): confirm to the customer AND move the application to CANCELLED, so
 * the read model reflects the terminal state (symmetric with the reject path). Without this the
 * process would end while the REST/UI status still read ORDERED.
 */
@Service
@Transactional
public class SendCancellationConfirmationService implements SendCancellationConfirmationUseCase {

    private final LeasingApplicationRepository repository;
    private final NotificationPort notification;

    public SendCancellationConfirmationService(
            LeasingApplicationRepository repository,
            NotificationPort notification) {
        this.repository = repository;
        this.notification = notification;
    }

    @Override
    public void sendCancellationConfirmation(ApplicationId id) {
        LeasingApplication application = repository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Unknown application " + id));
        notification.send("Your bike-leasing application has been cancelled", application);
        repository.save(application.cancel());
    }
}
