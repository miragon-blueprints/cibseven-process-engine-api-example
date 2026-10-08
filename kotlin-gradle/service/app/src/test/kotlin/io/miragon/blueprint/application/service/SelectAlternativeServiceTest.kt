package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.inbound.SelectAlternativeUseCase
import io.miragon.blueprint.application.port.outbound.BikePortfolioRepository
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.application.port.outbound.LeasingProcess
import io.miragon.blueprint.domain.bike.Bike
import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.leasing.testLeasingApplication
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class SelectAlternativeServiceTest {

    private val repository = mockk<LeasingApplicationRepository>()
    private val bikePortfolio = mockk<BikePortfolioRepository>()
    private val process = mockk<LeasingProcess>()
    private val underTest =
        SelectAlternativeService(repository = repository, bikePortfolio = bikePortfolio, process = process)

    @Test
    fun `an accepted alternative registers the new bike, points the application at it and completes the task`() {

        // given: an application whose requested bike was unavailable
        val application = testLeasingApplication()
        every { repository.findById(application.id) } returns application
        every { bikePortfolio.save(any()) } answers { firstArg() }
        every { repository.save(any()) } answers { firstArg() }
        every { process.completeAlternativeClarification(any(), any(), any()) } just Runs

        // when: an alternative bike is selected
        underTest.selectAlternative(
            SelectAlternativeUseCase.Command(application.id, alternativeFound = true, bikeId = BikeId("BIKE-ALT"), bikeModel = "Aero Road 700"),
        )

        // then: the alternative is registered in the portfolio, the application points at it and the task is completed
        verify { repository.findById(application.id) }
        verify { bikePortfolio.save(Bike(BikeId("BIKE-ALT"), "Aero Road 700")) }
        verify { repository.save(match { it.bikeId == BikeId("BIKE-ALT") }) }
        verify { process.completeAlternativeClarification(application.id, true, BikeId("BIKE-ALT")) }
        confirmVerified(repository, bikePortfolio, process)
    }

    @Test
    fun `no alternative completes the user task without touching the bike`() {

        // given: an application whose requested bike was unavailable
        val application = testLeasingApplication()
        every { repository.findById(application.id) } returns application
        every { process.completeAlternativeClarification(any(), any(), any()) } just Runs

        // when: no alternative is found
        underTest.selectAlternative(SelectAlternativeUseCase.Command(application.id, alternativeFound = false))

        // then: neither the portfolio nor the application is touched, and the task is completed as declined
        verify { repository.findById(application.id) }
        verify { process.completeAlternativeClarification(application.id, false, null) }
        confirmVerified(repository, bikePortfolio, process)
    }
}
