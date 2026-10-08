package io.miragon.blueprint.adapter.outbound.contract;

import io.miragon.blueprint.application.port.outbound.ContractPort;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import io.miragon.blueprint.domain.leasing.ContractId;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Blueprint contract adapter — logs instead of calling a real contract system. Swap this for a real
 * integration (issue / revoke a contract) without touching the application layer.
 */
@Component
public class LoggingContractAdapter implements ContractPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingContractAdapter.class);

    @Override
    public ContractId issueContract(ApplicationId id) {
        // A real system would create the contract in the contract system and return its reference.
        ContractId contractId = new ContractId("CONTRACT-" + UUID.randomUUID());
        log.info("Issued contract {} for application {}", contractId.value(), id.value());
        return contractId;
    }

    @Override
    public void revokeContract(ContractId contractId) {
        // A real system would revoke the issued contract here.
        log.info("Revoking contract {}", contractId.value());
    }
}
