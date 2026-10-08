package io.miragon.blueprint.application.service;

import io.miragon.blueprint.application.port.inbound.ListBikesQuery;
import io.miragon.blueprint.application.port.outbound.BikeDealerPort;
import io.miragon.blueprint.application.port.outbound.BikePortfolioRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ListBikesService implements ListBikesQuery {

    private final BikePortfolioRepository bikePortfolio;
    private final BikeDealerPort bikeDealer;

    public ListBikesService(BikePortfolioRepository bikePortfolio, BikeDealerPort bikeDealer) {
        this.bikePortfolio = bikePortfolio;
        this.bikeDealer = bikeDealer;
    }

    /**
     * Availability is asked from the dealer — the same source the process consults when ordering — so
     * the picker and the process can never disagree about whether a bike is in stock.
     */
    @Override
    public List<ListBikesQuery.Item> all() {
        // The portfolio returns the catalogue already ordered by id (ordering is the repository's job);
        // the service only enriches each bike with its dealer availability.
        return bikePortfolio
                .findAll()
                .stream()
                .map(bike -> new ListBikesQuery.Item(
                        bike.bikeId(), bike.model(), bikeDealer.checkAvailability(bike.bikeId())))
                .toList();
    }
}
