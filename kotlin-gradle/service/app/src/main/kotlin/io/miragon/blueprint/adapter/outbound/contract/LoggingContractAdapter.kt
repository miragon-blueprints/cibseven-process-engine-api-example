package io.miragon.blueprint.adapter.outbound.contract

import io.miragon.blueprint.application.port.outbound.ContractPort
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.ContractId
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Blueprint contract adapter — logs instead of calling a real contract system. Swap this for a real
 * integration (issue / revoke a contract) without touching the application layer.
 */
@Component
class LoggingContractAdapter : ContractPort {

    private val log = KotlinLogging.logger {}

    override fun issueContract(id: ApplicationId): ContractId {
        // A real system would create the contract in the contract system and return its reference.
        val contractId = ContractId("CONTRACT-${UUID.randomUUID()}")
        log.info { "Issued contract ${contractId.value} for application ${id.value}" }
        return contractId
    }

    override fun revokeContract(contractId: ContractId) {
        // A real system would revoke the issued contract here.
        log.info { "Revoking contract ${contractId.value}" }
    }
}
