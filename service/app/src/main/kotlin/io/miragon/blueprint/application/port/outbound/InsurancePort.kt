package io.miragon.blueprint.application.port.outbound

import io.miragon.blueprint.domain.leasing.ApplicationId

/**
 * Outbound port to the insurer: binds and revokes the policy that accompanies a leasing contract.
 * A real integration replaces the logging adapter without touching the application layer.
 */
interface InsurancePort {
    fun issuePolicy(id: ApplicationId)

    fun cancelPolicy(id: ApplicationId)
}
