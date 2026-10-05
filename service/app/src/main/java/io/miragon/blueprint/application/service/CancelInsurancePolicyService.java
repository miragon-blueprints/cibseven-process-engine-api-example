package io.miragon.blueprint.application.service;

import io.miragon.blueprint.application.port.inbound.CancelInsurancePolicyUseCase;
import io.miragon.blueprint.application.port.outbound.InsurancePort;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import org.springframework.stereotype.Service;

@Service
public class CancelInsurancePolicyService implements CancelInsurancePolicyUseCase {

    private final InsurancePort insurance;

    public CancelInsurancePolicyService(InsurancePort insurance) {
        this.insurance = insurance;
    }

    @Override
    public void cancelPolicy(ApplicationId id) {
        insurance.cancelPolicy(id);
    }
}
