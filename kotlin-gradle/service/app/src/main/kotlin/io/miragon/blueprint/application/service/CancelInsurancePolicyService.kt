package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.inbound.CancelInsurancePolicyUseCase
import io.miragon.blueprint.application.port.outbound.InsurancePort
import io.miragon.blueprint.domain.leasing.ApplicationId
import org.springframework.stereotype.Service

@Service
class CancelInsurancePolicyService(
    private val insurance: InsurancePort,
) : CancelInsurancePolicyUseCase {

    override fun cancelPolicy(id: ApplicationId) = insurance.cancelPolicy(id)
}
