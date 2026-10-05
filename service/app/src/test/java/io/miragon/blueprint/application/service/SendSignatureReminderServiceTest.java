package io.miragon.blueprint.application.service;

import static io.miragon.blueprint.domain.leasing.TestObjectBuilder.testLeasingApplication;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository;
import io.miragon.blueprint.application.port.outbound.NotificationPort;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import io.miragon.blueprint.domain.leasing.LeasingApplication;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SendSignatureReminderServiceTest {

    private final LeasingApplicationRepository repository = mock(LeasingApplicationRepository.class);
    private final NotificationPort notification = mock(NotificationPort.class);
    private final SendSignatureReminderService underTest = new SendSignatureReminderService(repository, notification);

    @Test
    void sendSignatureReminder_loads_the_application_and_reminds_the_customer() {

        // given: an application in the repository
        LeasingApplication application = testLeasingApplication().build();
        when(repository.findById(application.id())).thenReturn(Optional.of(application));

        // when: the signature reminder is sent
        underTest.sendSignatureReminder(application.id());

        // then: the application is loaded and the customer is reminded
        verify(repository).findById(application.id());
        verify(notification).send(anyString(), eq(application));
        verifyNoMoreInteractions(repository, notification);
    }

    @Test
    void sendSignatureReminder_fails_for_an_unknown_application() {

        // given: an application id the repository does not know
        ApplicationId unknownId = new ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174999"));
        when(repository.findById(unknownId)).thenReturn(Optional.empty());

        // when / then: the lookup fails and nobody is reminded
        assertThatThrownBy(() -> underTest.sendSignatureReminder(unknownId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unknown application " + unknownId);

        verify(repository).findById(unknownId);
        verifyNoInteractions(notification);
        verifyNoMoreInteractions(repository);
    }
}
