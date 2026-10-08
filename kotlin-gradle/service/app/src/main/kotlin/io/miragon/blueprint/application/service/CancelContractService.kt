package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.inbound.CancelContractUseCase
import io.miragon.blueprint.application.port.outbound.ContractPort
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.domain.leasing.ApplicationId
import org.springframework.stereotype.Service

@Service
class CancelContractService(
    private val repository: LeasingApplicationRepository,
    private val contract: ContractPort,
) : CancelContractUseCase {

    override fun cancelContract(id: ApplicationId) {
        val application = repository.findById(id) ?: error("Unknown application $id")
        // Revoke the contract the contract system issued earlier (its id is recorded on the application).
        val contractId = application.contractId ?: error("No contract issued for application $id")
        contract.revokeContract(contractId)
    }
}
