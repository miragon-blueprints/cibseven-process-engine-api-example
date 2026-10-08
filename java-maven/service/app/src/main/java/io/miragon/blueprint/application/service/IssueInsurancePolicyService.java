package io.miragon.blueprint.application.service;

import io.miragon.blueprint.application.port.inbound.IssueInsurancePolicyUseCase;
import io.miragon.blueprint.application.port.outbound.InsurancePort;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import org.springframework.stereotype.Service;

@Service
public class IssueInsurancePolicyService implements IssueInsurancePolicyUseCase {

    private final InsurancePort insurance;

    public IssueInsurancePolicyService(InsurancePort insurance) {
        this.insurance = insurance;
    }

    @Override
    public void issuePolicy(ApplicationId id) {
        insurance.issuePolicy(id);
    }
}
