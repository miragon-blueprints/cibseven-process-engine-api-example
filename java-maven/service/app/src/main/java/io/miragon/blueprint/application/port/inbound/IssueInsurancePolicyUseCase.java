package io.miragon.blueprint.application.port.inbound;

import io.miragon.blueprint.domain.leasing.ApplicationId;

/** Binds the insurance policy that accompanies the leasing contract. */
@FunctionalInterface
public interface IssueInsurancePolicyUseCase {

    void issuePolicy(ApplicationId id);
}
