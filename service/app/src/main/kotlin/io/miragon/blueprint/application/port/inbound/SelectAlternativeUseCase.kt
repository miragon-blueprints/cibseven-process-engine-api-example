package io.miragon.blueprint.application.port.inbound

import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.bike.BikeId

/**
 * Resolves the `Clarify alternative with customer` user task from the outside — the "external"
 * completion path via our own client, next to a human completing the deployed Camunda Form in the
 * Tasklist. When an alternative bike was found, the newly chosen bike is carried into the re-order.
 */
interface SelectAlternativeUseCase {
    fun selectAlternative(command: Command)

    data class Command(
        val applicationId: ApplicationId,
        val alternativeFound: Boolean,
        val bikeId: BikeId? = null,
        val bikeModel: String? = null,
    )
}
