package io.miragon.blueprint.application.service;

import io.miragon.blueprint.application.port.inbound.SignContractUseCase;
import io.miragon.blueprint.application.port.outbound.LeasingProcess;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SignContractService implements SignContractUseCase {

    private final LeasingProcess process;

    public SignContractService(LeasingProcess process) {
        this.process = process;
    }

    @Override
    public void signContract(ApplicationId id) {
        process.correlateContractSigned(id);
    }
}
