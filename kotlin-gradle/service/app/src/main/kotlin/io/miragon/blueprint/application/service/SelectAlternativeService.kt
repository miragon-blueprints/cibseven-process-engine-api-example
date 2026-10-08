package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.inbound.SelectAlternativeUseCase
import io.miragon.blueprint.application.port.outbound.BikePortfolioRepository
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.application.port.outbound.LeasingProcess
import io.miragon.blueprint.domain.bike.Bike
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SelectAlternativeService(
    private val repository: LeasingApplicationRepository,
    private val bikePortfolio: BikePortfolioRepository,
    private val process: LeasingProcess,
) : SelectAlternativeUseCase {

    override fun selectAlternative(command: SelectAlternativeUseCase.Command) {
        val application = repository.findById(command.applicationId) ?: error("Unknown application ${command.applicationId}")
        val alternativeBike = command.bikeId
        if (command.alternativeFound && alternativeBike != null) {
            // Register the chosen alternative in the portfolio (its model may be new), then point the application at it.
            command.bikeModel?.let { bikePortfolio.save(Bike(alternativeBike, it)) }
            repository.save(application.selectAlternative(alternativeBike))
        }
        process.completeAlternativeClarification(
            id = command.applicationId,
            alternativeFound = command.alternativeFound,
            bikeId = command.bikeId,
        )
    }
}
