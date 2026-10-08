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

    /** The whole catalogue — backs the `GET /api/bikes` picker. */
    fun findAll(): List<Bike>

    /**
     * Batch lookup of the given bikes. Used to resolve models for a page of applications in a single
     * query instead of one per row, so list endpoints don't teach an N+1.
     */
    fun findAllByIds(bikeIds: List<BikeId>): List<Bike>
}
