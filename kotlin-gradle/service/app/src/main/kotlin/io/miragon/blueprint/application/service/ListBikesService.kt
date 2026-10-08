package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.inbound.ListBikesQuery
import io.miragon.blueprint.application.port.outbound.BikeDealerPort
import io.miragon.blueprint.application.port.outbound.BikePortfolioRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ListBikesService(
    private val bikePortfolio: BikePortfolioRepository,
    private val bikeDealer: BikeDealerPort,
) : ListBikesQuery {

    /**
     * Availability is asked from the dealer — the same source the process consults when ordering — so
     * the picker and the process can never disagree about whether a bike is in stock.
     */
    override fun all(): List<ListBikesQuery.Item> =
        // The portfolio returns the catalogue already ordered by id; the service only enriches each
        // bike with its dealer availability. (Sorting here would emit a synthetic Comparator class in
        // the application.service package, which the architecture tests rightly reject.)
        bikePortfolio
            .findAll()
            .map { bike -> ListBikesQuery.Item(bike.bikeId, bike.model, bikeDealer.checkAvailability(bike.bikeId)) }
}
