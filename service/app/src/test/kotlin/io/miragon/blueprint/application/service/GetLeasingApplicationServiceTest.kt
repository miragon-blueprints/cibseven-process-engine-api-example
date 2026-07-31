package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.outbound.BikePortfolioRepository
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.bike.Bike
import io.miragon.blueprint.domain.leasing.testLeasingApplication
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class GetLeasingApplicationServiceTest {

    private val repository = mockk<LeasingApplicationRepository>()
    private val bikePortfolio = mockk<BikePortfolioRepository>()
    private val underTest = GetLeasingApplicationService(repository = repository, bikePortfolio = bikePortfolio)

    @Test
    fun `byId returns the application with its bike model resolved from the portfolio`() {

        // given: an application and its bike in the portfolio
        val application = testLeasingApplication()
        every { repository.findById(application.id) } returns application
        every { bikePortfolio.findByBikeId(application.bikeId) } returns Bike(application.bikeId, "Gravel Explorer 900")

        // when: the application is queried by id
        val result = underTest.byId(application.id)

        // then: the application and the resolved bike model are returned
        assertThat(result?.application).isEqualTo(application)
        assertThat(result?.bikeModel).isEqualTo("Gravel Explorer 900")
        verify { repository.findById(application.id) }
        verify { bikePortfolio.findByBikeId(application.bikeId) }
        confirmVerified(repository, bikePortfolio)
    }

    @Test
    fun `byId returns null when the application does not exist`() {

        // given: an unknown application id
        val id = ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))
        every { repository.findById(id) } returns null

        // when / then: the query returns null and the portfolio is never consulted
        assertThat(underTest.byId(id)).isNull()
        verify { repository.findById(id) }
        confirmVerified(repository, bikePortfolio)
    }
}
