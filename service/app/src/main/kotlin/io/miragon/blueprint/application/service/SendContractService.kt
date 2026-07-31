package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.inbound.SendContractUseCase
import io.miragon.blueprint.application.port.outbound.ContractPort
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.application.port.outbound.NotificationPort
import io.miragon.blueprint.domain.leasing.ApplicationId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SendContractService(
    private val repository: LeasingApplicationRepository,
    private val contract: ContractPort,
    private val notification: NotificationPort,
) : SendContractUseCase {

    override fun sendContract(id: ApplicationId) {
        val application = repository.findById(id) ?: error("Unknown application $id")
        // Issue the contract in the contract system and record its id on the application.
        val contractId = contract.issueContract(id)
        repository.save(application.withContract(contractId))
        notification.send("Please review and sign your leasing contract", application)
    }
}
