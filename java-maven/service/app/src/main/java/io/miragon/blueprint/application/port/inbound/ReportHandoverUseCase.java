package io.miragon.blueprint.application.port.inbound;

import io.miragon.blueprint.domain.leasing.ApplicationId;

public interface ReportHandoverUseCase {

    void reportHandover(ApplicationId id);
}
