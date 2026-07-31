package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.inbound.SubmitLeasingRequestUseCase
import io.miragon.blueprint.application.port.outbound.BikePortfolioRepository
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.application.port.outbound.LeasingProcess
import io.miragon.blueprint.domain.bike.Bike
import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.leasing.CustomerName
import io.miragon.blueprint.domain.leasing.Email
import io.miragon.blueprint.domain.leasing.LeasingStatus
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class SubmitLeasingRequestServiceTest {

    private val repository = mockk<LeasingApplicationRepository>()
    private val bikePortfolio = mockk<BikePortfolioRepository>()
    private val process = mockk<LeasingProcess>()
    private val clock = Clock.fixed(Instant.parse("2024-01-15T10:30:00Z"), ZoneOffset.UTC)
    private val underTest =
        SubmitLeasingRequestService(
            repository = repository,
            bikePortfolio = bikePortfolio,
            process = process,
            clock = clock,
        )

    @Test
    fun `submit registers the bike, persists a received application and starts the process`() {

        // given: a leasing-request command and stubbed out-ports
        val command =
            SubmitLeasingRequestUseCase.Command(
                customerName = CustomerName("John Doe"),
                email = Email("john.doe@test.com"),
                age = 35,
                monthlyNetIncome = 3500.0,
                bikeId = BikeId("BIKE-900"),
                bikeModel = "Gravel Explorer 900",
            )
        every { bikePortfolio.save(any()) } answers { firstArg() }
        every { repository.save(any()) } answers { firstArg() }
        every { process.submitRequest(any()) } just Runs

        // when: the use case is invoked
        val id = underTest.submit(command)

        // then: the bike is stored in the portfolio, a RECEIVED application referencing it is saved, and the process starts
        verify { bikePortfolio.save(Bike(BikeId("BIKE-900"), "Gravel Explorer 900")) }
        verify {
            repository.save(
                match {
                    it.id == id &&
                        it.status == LeasingStatus.RECEIVED &&
                        it.bikeId == BikeId("BIKE-900") &&
                        it.createdAt == LocalDateTime.now(clock)
                },
            )
        }
        verify { process.submitRequest(match { it.id == id }) }
        confirmVerified(repository, bikePortfolio, process)
    }
}
