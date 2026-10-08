package io.miragon.blueprint.adapter.outbound.db

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id

@Entity(name = "bike_portfolio")
data class BikeEntity(

    @Id
    @Column(name = "bike_id", nullable = false)
    val bikeId: String,

    @Column(name = "model", nullable = false)
    val model: String,
)
