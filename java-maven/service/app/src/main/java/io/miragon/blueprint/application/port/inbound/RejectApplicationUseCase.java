package io.miragon.blueprint.application.port.inbound;

import io.miragon.blueprint.domain.leasing.ApplicationId;

public interface RejectApplicationUseCase {

    void reject(ApplicationId id);
}
