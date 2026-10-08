package io.miragon.blueprint.adapter.outbound.notification;

import io.miragon.blueprint.application.port.outbound.NotificationPort;
import io.miragon.blueprint.domain.leasing.LeasingApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Blueprint notification adapter — logs the message instead of sending it. Swap this for an email /
 * messaging adapter without touching the application layer.
 */
@Component
public class LoggingNotificationAdapter implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationAdapter.class);

    @Override
    public void send(String subject, LeasingApplication application) {
        log.info("Notifying {}: {}", application.email().value(), subject);
    }
}
