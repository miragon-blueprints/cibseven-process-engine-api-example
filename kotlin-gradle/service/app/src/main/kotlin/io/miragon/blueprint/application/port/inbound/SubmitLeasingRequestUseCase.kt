package io.miragon.blueprint.application.port.inbound

import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.leasing.CustomerName
import io.miragon.blueprint.domain.leasing.Email

interface SubmitLeasingRequestUseCase {
    fun submit(command: Command): ApplicationId

    data class Command(
        val customerName: CustomerName,
        val email: Email,
        val age: Int,
        val monthlyNetIncome: Double,
        val bikeId: BikeId,
        val bikeModel: String,
    )
}
