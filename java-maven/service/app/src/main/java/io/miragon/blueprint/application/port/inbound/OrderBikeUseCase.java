package io.miragon.blueprint.application.port.inbound;

import io.miragon.blueprint.domain.bike.OrderId;
import io.miragon.blueprint.domain.leasing.ApplicationId;

public interface OrderBikeUseCase {

    OrderId orderBike(ApplicationId id);
}
