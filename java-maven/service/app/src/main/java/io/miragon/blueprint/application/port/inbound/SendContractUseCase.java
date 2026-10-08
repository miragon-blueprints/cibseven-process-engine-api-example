package io.miragon.blueprint.application.port.inbound;

import io.miragon.blueprint.domain.leasing.ApplicationId;

/** Sends the leasing contract to the customer for signature. */
@FunctionalInterface
public interface SendContractUseCase {

    void sendContract(ApplicationId id);
}
