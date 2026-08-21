package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.inbound.ListLeasingApplicationsQuery
import io.miragon.blueprint.application.port.outbound.BikePortfolioRepository
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.domain.bike.Bike
import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.leasing.LeasingStatus
import io.miragon.blueprint.domain.leasing.testLeasingApplication
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ListLeasingApplicationsServiceTest {

    private val repository = mockk<LeasingApplicationRepository>()
    private val bikePortfolio = mockk<BikePortfolioRepository>()
    private val underTest = ListLeasingApplicationsService(repository, bikePortfolio)

    @Test
    fun `maps a page of applications and resolves each bike model in one batch query`() {
        // given: a page with two applications on different bikes
        val a = testLeasingApplication(bikeId = BikeId("BIKE-900"), status = LeasingStatus.RECEIVED)
        val b = testLeasingApplication(bikeId = BikeId("BIKE-800"), status = LeasingStatus.ACTIVE)
        every { repository.findAll(any()) } returns
            LeasingApplicationRepository.Page(listOf(a, b), page = 0, size = 20, totalElements = 2, totalPages = 1)
        every { bikePortfolio.findAllByIds(any()) } returns
            listOf(Bike(BikeId("BIKE-900"), "Gravel Explorer 900"), Bike(BikeId("BIKE-800"), "Carbon Road 800"))

        // when: the first page is requested without a status filter
        val result = underTest.list(ListLeasingApplicationsQuery.Filter(status = null, page = 0, size = 20))

        // then: paging metadata is preserved and each item carries its resolved model
        assertThat(result.totalElements).isEqualTo(2)
        assertThat(result.totalPages).isEqualTo(1)
        assertThat(result.items).hasSize(2)
        assertThat(result.items[0].bikeModel).isEqualTo("Gravel Explorer 900")
        assertThat(result.items[1].bikeModel).isEqualTo("Carbon Road 800")
        // and: exactly one batch lookup was issued for the bikes, not one per row
        verify(exactly = 1) { bikePortfolio.findAllByIds(listOf(BikeId("BIKE-900"), BikeId("BIKE-800"))) }
    }

    @Test
    fun `passes the status filter through to the repository criteria`() {
        // given: a repository that records the criteria it was asked for
        every { repository.findAll(any()) } returns
            LeasingApplicationRepository.Page(emptyList(), page = 1, size = 5, totalElements = 0, totalPages = 0)
        every { bikePortfolio.findAllByIds(emptyList()) } returns emptyList()

        // when: a filtered, paged request is made
        underTest.list(ListLeasingApplicationsQuery.Filter(status = LeasingStatus.ACTIVE, page = 1, size = 5))

        // then: the exact criteria reach the outbound port
        verify {
            repository.findAll(LeasingApplicationRepository.Criteria(LeasingStatus.ACTIVE, page = 1, size = 5))
        }
        confirmVerified(repository)
    }

    @Test
    fun `leaves the model null when the bike is not in the portfolio`() {
        // given: an application whose bike is missing from the portfolio
        val orphan = testLeasingApplication(bikeId = BikeId("BIKE-GONE"))
        every { repository.findAll(any()) } returns
            LeasingApplicationRepository.Page(listOf(orphan), page = 0, size = 20, totalElements = 1, totalPages = 1)
        every { bikePortfolio.findAllByIds(any()) } returns emptyList()

        // when: the page is mapped
        val result = underTest.list(ListLeasingApplicationsQuery.Filter(status = null, page = 0, size = 20))

        // then: the missing model surfaces as null rather than throwing
        assertThat(result.items).hasSize(1)
        assertThat(result.items[0].bikeModel).isNull()
    }
}
