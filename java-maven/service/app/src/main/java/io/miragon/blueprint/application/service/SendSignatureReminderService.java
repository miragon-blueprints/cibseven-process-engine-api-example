package io.miragon.blueprint.application.service;

import io.miragon.blueprint.application.port.inbound.SendSignatureReminderUseCase;
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository;
import io.miragon.blueprint.application.port.outbound.NotificationPort;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import io.miragon.blueprint.domain.leasing.LeasingApplication;
import org.springframework.stereotype.Service;

@Service
public class SendSignatureReminderService implements SendSignatureReminderUseCase {

    private final LeasingApplicationRepository repository;
    private final NotificationPort notification;

    public SendSignatureReminderService(LeasingApplicationRepository repository, NotificationPort notification) {
        this.repository = repository;
        this.notification = notification;
    }

    @Override
    public void sendSignatureReminder(ApplicationId id) {
        LeasingApplication application = repository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Unknown application " + id));
        notification.send("Reminder: your leasing contract is still awaiting signature", application);
    }
}
