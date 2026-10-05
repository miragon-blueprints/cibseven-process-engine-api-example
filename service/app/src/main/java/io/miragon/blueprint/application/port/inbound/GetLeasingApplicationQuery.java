package io.miragon.blueprint.application.port.inbound;

import io.miragon.blueprint.domain.leasing.ApplicationId;
import io.miragon.blueprint.domain.leasing.LeasingApplication;
import java.util.Optional;

public interface GetLeasingApplicationQuery {

    /** The application with the given id, or {@link Optional#empty()} if there is none. */
    Optional<Result> byId(ApplicationId id);

    /**
     * The application together with the model of its bike, resolved from the portfolio.
     *
     * @param bikeModel the bike's model; {@code null} if the bike is not in the portfolio
     */
    record Result(
            LeasingApplication application,
            String bikeModel) {
    }
}
