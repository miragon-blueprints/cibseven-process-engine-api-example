package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.inbound.SelectAlternativeUseCase
import io.miragon.blueprint.application.port.outbound.BikePortfolioRepository
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.application.port.outbound.LeasingProcess
import io.miragon.blueprint.domain.bike.Bike
import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.testLeasingApplication
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.UUID

class SelectAlternativeServiceTest {

    private val repository = mockk<LeasingApplicationRepository>()
    private val bikePortfolio = mockk<BikePortfolioRepository>()
    private val process = mockk<LeasingProcess>()
    private val underTest =
        SelectAlternativeService(repository = repository, bikePortfolio = bikePortfolio, process = process)

    @Test
    fun `an accepted alternative registers the new bike and hands it to the process`() {

        // given: an application whose requested bike was unavailable
        val application = testLeasingApplication()
        every { repository.findById(application.id) } returns application
        every { bikePortfolio.save(any()) } answers { firstArg() }
        every { process.completeAlternativeClarification(any(), any(), any()) } just Runs

        // when: an alternative bike is selected
        underTest.selectAlternative(
            SelectAlternativeUseCase.Command(application.id, alternativeFound = true, bikeId = BikeId("BIKE-ALT"), bikeModel = "Aero Road 700"),
        )

        // then: the alternative is registered in the portfolio and the task is completed with it; the order step stores it
        verify { repository.findById(application.id) }
        verify { bikePortfolio.save(Bike(BikeId("BIKE-ALT"), "Aero Road 700")) }
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

    @Test
    fun `selectAlternative fails for an unknown application`() {

        // given: a decision for an application the repository does not know
        val unknownId = ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174999"))
        every { repository.findById(unknownId) } returns null

        // when / then: the lookup fails before the portfolio, the application or the task is touched
        val command =
            SelectAlternativeUseCase.Command(unknownId, alternativeFound = true, bikeId = BikeId("BIKE-ALT"), bikeModel = "Aero Road 700")
        assertThatThrownBy { underTest.selectAlternative(command) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Unknown application $unknownId")
        verify { repository.findById(unknownId) }
        confirmVerified(repository, bikePortfolio, process)
    }
}
