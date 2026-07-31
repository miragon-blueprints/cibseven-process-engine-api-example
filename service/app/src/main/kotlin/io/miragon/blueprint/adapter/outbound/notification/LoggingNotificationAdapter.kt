package io.miragon.blueprint.adapter.outbound.notification

import io.miragon.blueprint.application.port.outbound.NotificationPort
import io.miragon.blueprint.domain.leasing.LeasingApplication
import mu.KotlinLogging
import org.springframework.stereotype.Component

/**
 * Blueprint notification adapter — logs the message instead of sending it. Swap this for an email /
 * messaging adapter without touching the application layer.
 */
@Component
class LoggingNotificationAdapter : NotificationPort {

    private val log = KotlinLogging.logger {}

    override fun send(subject: String, application: LeasingApplication) {
        log.info { "Notifying ${application.email.value}: $subject" }
    }
}
