package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.inbound.GetLeasingApplicationQuery
import io.miragon.blueprint.application.port.outbound.BikePortfolioRepository
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.domain.leasing.ApplicationId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetLeasingApplicationService(
    private val repository: LeasingApplicationRepository,
    private val bikePortfolio: BikePortfolioRepository,
) : GetLeasingApplicationQuery {

    override fun byId(id: ApplicationId): GetLeasingApplicationQuery.Result? {
        val application = repository.findById(id) ?: return null
        val bike = bikePortfolio.findByBikeId(application.bikeId)
        return GetLeasingApplicationQuery.Result(application, bike?.model)
    }
}
