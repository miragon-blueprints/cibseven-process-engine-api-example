package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.inbound.ListLeasingApplicationsQuery
import io.miragon.blueprint.application.port.outbound.BikePortfolioRepository
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ListLeasingApplicationsService(
    private val repository: LeasingApplicationRepository,
    private val bikePortfolio: BikePortfolioRepository,
) : ListLeasingApplicationsQuery {

    override fun list(filter: ListLeasingApplicationsQuery.Filter): ListLeasingApplicationsQuery.Page {
        val page = repository.findAll(
            LeasingApplicationRepository.Criteria(
                status = filter.status,
                page = filter.page,
                size = filter.size,
            ),
        )
        // Resolve every bike model in one batch query rather than one per row.
        val models = bikePortfolio
            .findAllByIds(page.items.map { it.bikeId })
            .associate { it.bikeId to it.model }
        return ListLeasingApplicationsQuery.Page(
            items = page.items.map { application ->
                ListLeasingApplicationsQuery.Item(
                    applicationId = application.id,
                    customerName = application.customerName,
                    bikeId = application.bikeId,
                    bikeModel = models[application.bikeId],
                    status = application.status,
                    createdAt = application.createdAt,
                )
            },
            page = page.page,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
    }
}
