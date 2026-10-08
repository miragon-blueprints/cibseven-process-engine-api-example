package io.miragon.blueprint.domain.leasing

import io.miragon.blueprint.domain.bike.BikeId
import java.time.LocalDateTime

/**
 * A leasing application parked on the `Clarify alternative with customer` user task because the
 * requested bike was unavailable — the read model behind the back-office inbox. It carries what an
 * agent needs to pick up the case (who, which bike, since when), but deliberately no engine task id:
 * the case is resolved through the domain, correlated by application id.
 */
data class PendingClarification(
    val applicationId: ApplicationId,
    val customerName: CustomerName,
    val requestedBikeId: BikeId,
    val requestedBikeModel: String?,
    val waitingSince: LocalDateTime,
)
