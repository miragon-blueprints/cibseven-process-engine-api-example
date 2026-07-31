package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.inbound.SubmitLeasingRequestUseCase
import io.miragon.blueprint.application.port.outbound.BikePortfolioRepository
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.application.port.outbound.LeasingProcess
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.bike.Bike
import io.miragon.blueprint.domain.leasing.LeasingApplication
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
@Transactional
class SubmitLeasingRequestService(
    private val repository: LeasingApplicationRepository,
    private val bikePortfolio: BikePortfolioRepository,
    private val process: LeasingProcess,
    private val clock: Clock,
) : SubmitLeasingRequestUseCase {

    override fun submit(command: SubmitLeasingRequestUseCase.Command): ApplicationId {
        // The bike's model is kept in the portfolio; the application only references the bike by id.
        bikePortfolio.save(Bike(command.bikeId, command.bikeModel))
        val application =
            LeasingApplication.receive(
                id = ApplicationId.new(),
                customerName = command.customerName,
                email = command.email,
                age = command.age,
                monthlyNetIncome = command.monthlyNetIncome,
                bikeId = command.bikeId,
                createdAt = LocalDateTime.now(clock),
            )
        repository.save(application)
        process.submitRequest(application)
        return application.id
    }
}
