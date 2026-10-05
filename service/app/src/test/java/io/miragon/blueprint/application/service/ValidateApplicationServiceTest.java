package io.miragon.blueprint.application.service;

import static io.miragon.blueprint.domain.leasing.TestObjectBuilder.testLeasingApplication;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import io.miragon.blueprint.domain.leasing.ApplicationInvalidException;
import io.miragon.blueprint.domain.leasing.LeasingApplication;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ValidateApplicationServiceTest {

    private final LeasingApplicationRepository repository = mock(LeasingApplicationRepository.class);
    private final ValidateApplicationService underTest = new ValidateApplicationService(repository);

    @Test
    void validate_loads_a_well_formed_application_without_error() {

        // given: a valid, solvent application in the repository
        LeasingApplication application = testLeasingApplication().build();
        when(repository.findById(application.id())).thenReturn(Optional.of(application));

        // when: the application is validated
        underTest.validate(application.id());

        // then: the application was loaded and accepted
        verify(repository).findById(application.id());
        verifyNoMoreInteractions(repository);
    }

    @Test
    void validate_rejects_an_application_without_income() {

        // given: an application with zero monthly net income
        LeasingApplication application = testLeasingApplication().monthlyNetIncome(0.0).build();
        when(repository.findById(application.id())).thenReturn(Optional.of(application));

        // when / then: validation surfaces the application as invalid
        assertThatThrownBy(() -> underTest.validate(application.id()))
                .isInstanceOf(ApplicationInvalidException.class)
                .hasFieldOrPropertyWithValue("applicationId", application.id());
        verify(repository).findById(application.id());
        verifyNoMoreInteractions(repository);
    }

    @Test
    void validate_fails_for_an_unknown_application() {

        // given: an application id the repository does not know
        ApplicationId unknownId = new ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174999"));
        when(repository.findById(unknownId)).thenReturn(Optional.empty());

        // when / then: the lookup fails instead of reporting the application as invalid
        assertThatThrownBy(() -> underTest.validate(unknownId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unknown application " + unknownId);

        verify(repository).findById(unknownId);
        verifyNoMoreInteractions(repository);
    }
}
