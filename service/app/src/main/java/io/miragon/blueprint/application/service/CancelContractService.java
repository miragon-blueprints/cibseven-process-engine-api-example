package io.miragon.blueprint.application.service;

import io.miragon.blueprint.application.port.inbound.CancelContractUseCase;
import io.miragon.blueprint.application.port.outbound.ContractPort;
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import io.miragon.blueprint.domain.leasing.ContractId;
import io.miragon.blueprint.domain.leasing.LeasingApplication;
import org.springframework.stereotype.Service;

@Service
public class CancelContractService implements CancelContractUseCase {

    private final LeasingApplicationRepository repository;
    private final ContractPort contract;

    public CancelContractService(LeasingApplicationRepository repository, ContractPort contract) {
        this.repository = repository;
        this.contract = contract;
    }

    @Override
    public void cancelContract(ApplicationId id) {
        LeasingApplication application = repository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Unknown application " + id));
        // Revoke the contract the contract system issued earlier (its id is recorded on the application).
        ContractId contractId = application.contractId();
        if (contractId == null) {
            throw new IllegalStateException("No contract issued for application " + id);
        }
        contract.revokeContract(contractId);
    }
}
