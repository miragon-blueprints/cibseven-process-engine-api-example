package io.miragon.blueprint.application.port.outbound

import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.ContractId

/**
 * Outbound port to the (external) contract system: issues the leasing contract and revokes it as part
 * of the SAGA compensation. (Notifying the customer to sign is a separate [NotificationPort] concern.)
 * A real integration replaces the logging adapter without touching the application layer.
 */
interface ContractPort {
    fun issueContract(id: ApplicationId): ContractId

    fun revokeContract(contractId: ContractId)
}
