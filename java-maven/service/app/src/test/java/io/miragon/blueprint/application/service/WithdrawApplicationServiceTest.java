package io.miragon.blueprint.application.service;

import static io.miragon.blueprint.domain.leasing.TestObjectBuilder.testLeasingApplication;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository;
import io.miragon.blueprint.application.port.outbound.LeasingProcess;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import io.miragon.blueprint.domain.leasing.LeasingApplication;
import io.miragon.blueprint.domain.leasing.LeasingStatus;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WithdrawApplicationServiceTest {

    private final LeasingProcess process = mock(LeasingProcess.class);
    private final LeasingApplicationRepository repository = mock(LeasingApplicationRepository.class);
    private final WithdrawApplicationService underTest = new WithdrawApplicationService(process, repository);

    @Test
    void withdraw_correlates_the_message_and_persists_the_WITHDRAWN_status() {

        // given: a handed-over application
        ApplicationId id = new ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        LeasingApplication application = testLeasingApplication().id(id).status(LeasingStatus.HANDED_OVER).build();
        when(repository.findById(id)).thenReturn(Optional.of(application));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when: the application is withdrawn
        underTest.withdraw(id);

        // then: the message is correlated and the application is persisted with WITHDRAWN
        verify(process).correlateApplicationWithdrawn(id);
        verify(repository).findById(id);
        verify(repository).save(argThat(a -> a.status() == LeasingStatus.WITHDRAWN));
        verifyNoMoreInteractions(process, repository);
    }

    @Test
    void withdraw_does_not_persist_when_correlation_fails() {

        // given: a correlation that throws
        ApplicationId id = new ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174001"));
        doThrow(new RuntimeException("no token")).when(process).correlateApplicationWithdrawn(id);

        // when / then: the exception propagates without touching the repository
        assertThatThrownBy(() -> underTest.withdraw(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("no token");

        verify(process).correlateApplicationWithdrawn(id);
        verify(repository, never()).findById(any());
        verify(repository, never()).save(any());
        verifyNoMoreInteractions(process, repository);
    }

    @Test
    void withdraw_fails_for_an_unknown_application() {

        // given: a correlated withdrawal for an application the repository does not know
        ApplicationId unknownId = new ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174999"));
        when(repository.findById(unknownId)).thenReturn(Optional.empty());

        // when / then: the lookup fails and nothing is persisted
        assertThatThrownBy(() -> underTest.withdraw(unknownId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unknown application " + unknownId);

        verify(process).correlateApplicationWithdrawn(unknownId);
        verify(repository).findById(unknownId);
        verify(repository, never()).save(any());
        verifyNoMoreInteractions(process, repository);
    }
}
