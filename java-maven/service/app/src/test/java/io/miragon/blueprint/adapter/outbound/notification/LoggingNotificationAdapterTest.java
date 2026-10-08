package io.miragon.blueprint.adapter.outbound.notification;

import static io.miragon.blueprint.domain.leasing.TestObjectBuilder.testLeasingApplication;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.miragon.blueprint.domain.leasing.LeasingApplication;
import org.junit.jupiter.api.Test;

class LoggingNotificationAdapterTest {

    private final LoggingNotificationAdapter underTest = new LoggingNotificationAdapter();

    @Test
    void send_logs_the_notification_without_error() {

        // given: a subject and an application
        LeasingApplication application = testLeasingApplication().build();

        // when / then: sending the notification completes without throwing
        assertThatCode(() -> underTest.send("Please review and sign your leasing contract", application))
                .doesNotThrowAnyException();
    }
}
