package io.miragon.blueprint.application.port.inbound;

import io.miragon.blueprint.domain.leasing.ApplicationId;

/** Compensation: revokes an insurance policy that was already issued. */
@FunctionalInterface
public interface CancelInsurancePolicyUseCase {

    void cancelPolicy(ApplicationId id);
}
