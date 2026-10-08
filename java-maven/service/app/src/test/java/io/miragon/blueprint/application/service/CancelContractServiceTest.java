package io.miragon.blueprint.application.service;

import static io.miragon.blueprint.domain.leasing.TestObjectBuilder.testLeasingApplication;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.miragon.blueprint.application.port.outbound.ContractPort;
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import io.miragon.blueprint.domain.leasing.ContractId;
import io.miragon.blueprint.domain.leasing.LeasingApplication;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CancelContractServiceTest {

    private final LeasingApplicationRepository repository = mock(LeasingApplicationRepository.class);
    private final ContractPort contract = mock(ContractPort.class);
    private final CancelContractService underTest = new CancelContractService(repository, contract);

    @Test
    void cancelContract_revokes_the_contract_recorded_on_the_application() {

        // given: an application that already carries an issued contract
        LeasingApplication application = testLeasingApplication().contractId(new ContractId("CONTRACT-1")).build();
        when(repository.findById(application.id())).thenReturn(Optional.of(application));

        // when: the contract is cancelled as part of the compensation
        underTest.cancelContract(application.id());

        // then: the recorded contract id is revoked in the contract system
        verify(repository).findById(application.id());
        verify(contract).revokeContract(new ContractId("CONTRACT-1"));
        verifyNoMoreInteractions(repository, contract);
    }

    @Test
    void cancelContract_fails_when_the_application_is_unknown() {

        // given: an id the repository cannot resolve
        ApplicationId id = ApplicationId.of("123e4567-e89b-12d3-a456-426614174000");
        when(repository.findById(id)).thenReturn(Optional.empty());

        // when/then: cancellation fails and no contract is touched
        assertThatThrownBy(() -> underTest.cancelContract(id))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unknown application " + id);
        verify(repository).findById(id);
        verifyNoMoreInteractions(repository, contract);
    }

    @Test
    void cancelContract_fails_when_no_contract_was_issued_for_the_application() {

        // given: an application that never received a contract
        LeasingApplication application = testLeasingApplication().contractId(null).build();
        when(repository.findById(application.id())).thenReturn(Optional.of(application));

        // when/then: cancellation fails and no contract is touched
        assertThatThrownBy(() -> underTest.cancelContract(application.id()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No contract issued for application " + application.id());
        verify(repository).findById(application.id());
        verifyNoMoreInteractions(repository, contract);
    }
}
