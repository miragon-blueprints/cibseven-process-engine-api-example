package io.miragon.blueprint.application.service;

import io.miragon.blueprint.application.port.inbound.ListLeasingApplicationsQuery;
import io.miragon.blueprint.application.port.outbound.BikePortfolioRepository;
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository;
import io.miragon.blueprint.domain.bike.BikeId;
import io.miragon.blueprint.domain.leasing.LeasingApplication;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ListLeasingApplicationsService implements ListLeasingApplicationsQuery {

    private final LeasingApplicationRepository repository;
    private final BikePortfolioRepository bikePortfolio;

    public ListLeasingApplicationsService(
            LeasingApplicationRepository repository,
            BikePortfolioRepository bikePortfolio) {
        this.repository = repository;
        this.bikePortfolio = bikePortfolio;
    }

    @Override
    public ListLeasingApplicationsQuery.Page list(ListLeasingApplicationsQuery.Filter filter) {
        LeasingApplicationRepository.Page page = repository.findAll(
                new LeasingApplicationRepository.Criteria(
                        filter.status(),
                        filter.page(),
                        filter.size()));
        // Resolve every bike model in one batch query rather than one per row.
        Map<BikeId, String> models = new HashMap<>();
        bikePortfolio
                .findAllByIds(page.items().stream().map(LeasingApplication::bikeId).toList())
                .forEach(bike -> models.put(bike.bikeId(), bike.model()));
        return new ListLeasingApplicationsQuery.Page(
                page.items().stream()
                        .map(application -> new ListLeasingApplicationsQuery.Item(
                                application.id(),
                                application.customerName(),
                                application.bikeId(),
                                models.get(application.bikeId()),
                                application.status(),
                                application.createdAt()))
                        .toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages());
    }
}
