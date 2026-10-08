package io.miragon.blueprint.application.service;

import io.miragon.blueprint.application.port.inbound.SendContractUseCase;
import io.miragon.blueprint.application.port.outbound.ContractPort;
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository;
import io.miragon.blueprint.application.port.outbound.NotificationPort;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import io.miragon.blueprint.domain.leasing.ContractId;
import io.miragon.blueprint.domain.leasing.LeasingApplication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SendContractService implements SendContractUseCase {

    private final LeasingApplicationRepository repository;
    private final ContractPort contract;
    private final NotificationPort notification;

    public SendContractService(
            LeasingApplicationRepository repository,
            ContractPort contract,
            NotificationPort notification) {
        this.repository = repository;
        this.contract = contract;
        this.notification = notification;
    }

    @Override
    public void sendContract(ApplicationId id) {
        LeasingApplication application = repository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Unknown application " + id));
        // Issue the contract in the contract system and record its id on the application.
        ContractId contractId = contract.issueContract(id);
        repository.save(application.withContract(contractId));
        notification.send("Please review and sign your leasing contract", application);
    }
}
