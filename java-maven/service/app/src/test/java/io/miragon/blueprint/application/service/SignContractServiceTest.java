package io.miragon.blueprint.application.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import io.miragon.blueprint.application.port.outbound.LeasingProcess;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SignContractServiceTest {

    private final LeasingProcess process = mock(LeasingProcess.class);
    private final SignContractService underTest = new SignContractService(process);

    @Test
    void signContract_correlates_the_contract_signed_message() {

        // given: an application id
        ApplicationId id = new ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));

        // when: the contract is signed
        underTest.signContract(id);

        // then: the contract-signed message is correlated for that application
        verify(process).correlateContractSigned(id);
        verifyNoMoreInteractions(process);
    }
}
