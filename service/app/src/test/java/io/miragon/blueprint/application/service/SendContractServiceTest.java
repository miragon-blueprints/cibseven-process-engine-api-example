package io.miragon.blueprint.application.service;

import static io.miragon.blueprint.domain.leasing.TestObjectBuilder.testLeasingApplication;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.miragon.blueprint.application.port.outbound.ContractPort;
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository;
import io.miragon.blueprint.application.port.outbound.NotificationPort;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import io.miragon.blueprint.domain.leasing.ContractId;
import io.miragon.blueprint.domain.leasing.LeasingApplication;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SendContractServiceTest {

    private final LeasingApplicationRepository repository = mock(LeasingApplicationRepository.class);
    private final ContractPort contract = mock(ContractPort.class);
    private final NotificationPort notification = mock(NotificationPort.class);
    private final SendContractService underTest = new SendContractService(repository, contract, notification);

    @Test
    void sendContract_issues_the_contract_records_its_id_on_the_application_and_notifies_the_customer() {

        // given: an application whose contract the contract system will issue
        LeasingApplication application = testLeasingApplication().build();
        when(repository.findById(application.id())).thenReturn(Optional.of(application));
        when(contract.issueContract(application.id())).thenReturn(new ContractId("CONTRACT-1"));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when: the contract is sent
        underTest.sendContract(application.id());

        // then: the contract is issued, its id is stored on the application and the customer is asked to sign
        verify(repository).findById(application.id());
        verify(contract).issueContract(application.id());
        verify(repository).save(argThat(a -> new ContractId("CONTRACT-1").equals(a.contractId())));
        verify(notification).send(anyString(), eq(application));
        verifyNoMoreInteractions(repository, contract, notification);
    }

    @Test
    void sendContract_fails_for_an_unknown_application() {

        // given: an application id the repository does not know
        ApplicationId unknownId = new ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174999"));
        when(repository.findById(unknownId)).thenReturn(Optional.empty());

        // when / then: the lookup fails before a contract is issued, persisted or announced
        assertThatThrownBy(() -> underTest.sendContract(unknownId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unknown application " + unknownId);

        verify(repository).findById(unknownId);
        verify(repository, never()).save(any());
        verifyNoInteractions(contract, notification);
        verifyNoMoreInteractions(repository);
    }
}
