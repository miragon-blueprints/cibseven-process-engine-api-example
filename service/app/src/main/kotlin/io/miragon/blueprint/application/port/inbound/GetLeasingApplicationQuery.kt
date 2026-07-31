package io.miragon.blueprint.application.port.inbound

import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.LeasingApplication

interface GetLeasingApplicationQuery {
    fun byId(id: ApplicationId): Result?

    /** The application together with the model of its bike, resolved from the portfolio. */
    data class Result(
        val application: LeasingApplication,
        val bikeModel: String?,
    )
}
