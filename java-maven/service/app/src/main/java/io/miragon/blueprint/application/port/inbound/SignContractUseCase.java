package io.miragon.blueprint.application.port.inbound;

import io.miragon.blueprint.domain.leasing.ApplicationId;

public interface SignContractUseCase {

    void signContract(ApplicationId id);
}
