package io.miragon.blueprint.adapter.outbound.db

import io.miragon.blueprint.domain.leasing.LeasingStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import java.time.LocalDateTime
import java.util.UUID

@Entity(name = "leasing_application")
data class LeasingApplicationEntity(

    @Id
    @Column(name = "application_id", nullable = false)
    val applicationId: UUID,

    @Column(name = "customer_name", nullable = false)
    val customerName: String,

    @Column(name = "email", nullable = false)
    val email: String,

    @Column(name = "age", nullable = false)
    val age: Int,

    @Column(name = "monthly_net_income", nullable = false)
    val monthlyNetIncome: Double,

    @Column(name = "bike_id", nullable = false)
    val bikeId: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: LeasingStatus,

    @Column(name = "order_id")
    val orderId: String? = null,

    @Column(name = "contract_id")
    val contractId: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime,
)
