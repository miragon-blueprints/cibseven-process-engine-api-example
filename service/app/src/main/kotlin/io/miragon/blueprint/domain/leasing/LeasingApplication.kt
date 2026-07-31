package io.miragon.blueprint.domain.leasing

import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.bike.OrderId
import java.time.LocalDateTime

/**
 * Aggregate root of the bike-leasing domain. All state transitions return a copy, so an instance is
 * never mutated in place — the calling service persists the returned copy.
 */
data class LeasingApplication(
    val id: ApplicationId,
    val customerName: CustomerName,
    val email: Email,
    val age: Int,
    val monthlyNetIncome: Double,
    val bikeId: BikeId,
    val status: LeasingStatus,
    val createdAt: LocalDateTime,
    val orderId: OrderId? = null,
    val contractId: ContractId? = null,
) {

    /** Ensures the application is fundamentally processable; throws otherwise. */
    fun validate(): LeasingApplication {
        if (monthlyNetIncome <= 0.0) {
            throw ApplicationInvalidException(id, "monthly net income must be greater than zero")
        }
        return this
    }

    /** Records the contract the contract system issued for this application. */
    fun withContract(contractId: ContractId): LeasingApplication =
        copy(contractId = contractId)

    /** Records the placed order on the application and moves it to ORDERED. */
    fun documentOrder(orderId: OrderId): LeasingApplication =
        copy(orderId = orderId, status = LeasingStatus.ORDERED)

    /** Records that the customer accepted a different bike after the requested one was unavailable. */
    fun selectAlternative(bikeId: BikeId): LeasingApplication =
        copy(bikeId = bikeId)

    fun activate(): LeasingApplication = copy(status = LeasingStatus.ACTIVE)

    fun reject(): LeasingApplication = copy(status = LeasingStatus.REJECTED)

    fun cancel(): LeasingApplication = copy(status = LeasingStatus.CANCELLED)

    companion object {
        fun receive(
            id: ApplicationId,
            customerName: CustomerName,
            email: Email,
            age: Int,
            monthlyNetIncome: Double,
            bikeId: BikeId,
            createdAt: LocalDateTime,
        ): LeasingApplication =
            LeasingApplication(
                id = id,
                customerName = customerName,
                email = email,
                age = age,
                monthlyNetIncome = monthlyNetIncome,
                bikeId = bikeId,
                status = LeasingStatus.RECEIVED,
                createdAt = createdAt,
            )
    }
}
