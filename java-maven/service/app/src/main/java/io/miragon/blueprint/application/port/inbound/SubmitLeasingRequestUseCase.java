package io.miragon.blueprint.application.port.inbound;

import io.miragon.blueprint.domain.bike.BikeId;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import io.miragon.blueprint.domain.leasing.CustomerName;
import io.miragon.blueprint.domain.leasing.Email;

public interface SubmitLeasingRequestUseCase {

    ApplicationId submit(Command command);

    record Command(
            CustomerName customerName,
            Email email,
            int age,
            double monthlyNetIncome,
            BikeId bikeId,
            String bikeModel) {
    }
}
