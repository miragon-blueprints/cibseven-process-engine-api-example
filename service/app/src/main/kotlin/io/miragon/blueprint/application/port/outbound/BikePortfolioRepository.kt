package io.miragon.blueprint.application.port.outbound

import io.miragon.blueprint.domain.bike.Bike
import io.miragon.blueprint.domain.bike.BikeId

/**
 * Persistence port for MiraVelo's bike portfolio — the catalogue of bikes and their models, kept as a
 * separate aggregate from the leasing application (which only references a bike by its [BikeId]).
 */
interface BikePortfolioRepository {
    fun save(bike: Bike): Bike

    fun findByBikeId(bikeId: BikeId): Bike?
}
