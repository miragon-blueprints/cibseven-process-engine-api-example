package io.miragon.blueprint.adapter.outbound.db

import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.leasing.ContractId
import io.miragon.blueprint.domain.leasing.CustomerName
import io.miragon.blueprint.domain.leasing.Email
import io.miragon.blueprint.domain.leasing.LeasingApplication
import io.miragon.blueprint.domain.bike.OrderId

object LeasingApplicationEntityMapper {

    fun toDomain(entity: LeasingApplicationEntity): LeasingApplication =
        LeasingApplication(
            id = ApplicationId(entity.applicationId),
            customerName = CustomerName(entity.customerName),
            email = Email(entity.email),
            age = entity.age,
            monthlyNetIncome = entity.monthlyNetIncome,
            bikeId = BikeId(entity.bikeId),
            status = entity.status,
            createdAt = entity.createdAt,
            orderId = entity.orderId?.let { OrderId(it) },
            contractId = entity.contractId?.let { ContractId(it) },
        )

    fun toEntity(domain: LeasingApplication): LeasingApplicationEntity =
        LeasingApplicationEntity(
            applicationId = domain.id.value,
            customerName = domain.customerName.value,
            email = domain.email.value,
            age = domain.age,
            monthlyNetIncome = domain.monthlyNetIncome,
            bikeId = domain.bikeId.value,
            status = domain.status,
            createdAt = domain.createdAt,
            orderId = domain.orderId?.value,
            contractId = domain.contractId?.value,
        )
}
