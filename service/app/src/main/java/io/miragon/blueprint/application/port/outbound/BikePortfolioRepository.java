package io.miragon.blueprint.application.port.outbound;

import io.miragon.blueprint.domain.bike.Bike;
import io.miragon.blueprint.domain.bike.BikeId;
import java.util.List;
import java.util.Optional;

/**
 * Persistence port for MiraVelo's bike portfolio — the catalogue of bikes and their models, kept as a
 * separate aggregate from the leasing application (which only references a bike by its {@link BikeId}).
 */
public interface BikePortfolioRepository {

    Bike save(Bike bike);

    /** The bike with the given id, or {@link Optional#empty()} if it is not in the portfolio. */
    Optional<Bike> findByBikeId(BikeId bikeId);

    /** The whole catalogue — backs the {@code GET /api/bikes} picker. */
    List<Bike> findAll();

    /**
     * Batch lookup of the given bikes. Used to resolve models for a page of applications in a single
     * query instead of one per row, so list endpoints don't teach an N+1.
     */
    List<Bike> findAllByIds(List<BikeId> bikeIds);
}
