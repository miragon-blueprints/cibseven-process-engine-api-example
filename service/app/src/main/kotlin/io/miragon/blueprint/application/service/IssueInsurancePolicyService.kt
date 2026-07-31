package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.inbound.IssueInsurancePolicyUseCase
import io.miragon.blueprint.application.port.outbound.InsurancePort
import io.miragon.blueprint.domain.leasing.ApplicationId
import org.springframework.stereotype.Service

@Service
class IssueInsurancePolicyService(
    private val insurance: InsurancePort,
) : IssueInsurancePolicyUseCase {

    override fun issuePolicy(id: ApplicationId) = insurance.issuePolicy(id)
}
