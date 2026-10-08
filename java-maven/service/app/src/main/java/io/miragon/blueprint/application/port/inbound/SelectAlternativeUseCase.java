package io.miragon.blueprint.application.port.inbound;

import io.miragon.blueprint.domain.bike.BikeId;
import io.miragon.blueprint.domain.leasing.ApplicationId;

/**
 * Resolves the {@code Clarify alternative with customer} user task from the outside — the "external"
 * completion path via our own client, next to a human completing the deployed Camunda Form in the
 * Tasklist. When an alternative bike was found, the newly chosen bike is carried into the re-order.
 */
public interface SelectAlternativeUseCase {

    void selectAlternative(Command command);

    /**
     * @param bikeId    the newly chosen bike; {@code null} when no alternative was found
     * @param bikeModel the newly chosen bike's model; {@code null} when no alternative was found
     */
    record Command(
            ApplicationId applicationId,
            boolean alternativeFound,
            BikeId bikeId,
            String bikeModel) {
    }
}
