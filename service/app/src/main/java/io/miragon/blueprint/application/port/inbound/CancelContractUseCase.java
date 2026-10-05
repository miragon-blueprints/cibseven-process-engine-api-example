package io.miragon.blueprint.application.port.inbound;

import io.miragon.blueprint.domain.leasing.ApplicationId;

/** Compensation: revokes a contract that was already sent. */
@FunctionalInterface
public interface CancelContractUseCase {

    void cancelContract(ApplicationId id);
}
