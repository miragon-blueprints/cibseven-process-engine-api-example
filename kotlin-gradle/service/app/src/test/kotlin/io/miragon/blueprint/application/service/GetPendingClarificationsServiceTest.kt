package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.outbound.BikePortfolioRepository
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.application.port.outbound.TaskInboxPort
import io.miragon.blueprint.domain.bike.Bike
import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.testLeasingApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class GetPendingClarificationsServiceTest {

    private val taskInbox = mockk<TaskInboxPort>()
    private val repository = mockk<LeasingApplicationRepository>()
    private val bikePortfolio = mockk<BikePortfolioRepository>()
    private val underTest = GetPendingClarificationsService(taskInbox, repository, bikePortfolio)

    private val id = ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))
    private val waitingSince = LocalDateTime.of(2026, 8, 17, 9, 30)

    @Test
    fun `enriches each open task with its application and requested bike model`() {
        // given: one open clarification whose application requested BIKE-OOS
        val application = testLeasingApplication(id = id, bikeId = BikeId("BIKE-OOS"))
        every { taskInbox.findOpenClarifications() } returns
            listOf(TaskInboxPort.OpenClarification(id, waitingSince))
        every { repository.findById(id) } returns application
        every { bikePortfolio.findAllByIds(listOf(BikeId("BIKE-OOS"))) } returns
            listOf(Bike(BikeId("BIKE-OOS"), "Mountain Trail 600"))

        // when: the inbox is read
        val result = underTest.pending()

        // then: the pending clarification carries who, which bike (+ model) and since when — but no task id
        assertThat(result).hasSize(1)
        with(result.single()) {
            assertThat(applicationId).isEqualTo(id)
            assertThat(customerName).isEqualTo(application.customerName)
            assertThat(requestedBikeId).isEqualTo(BikeId("BIKE-OOS"))
            assertThat(requestedBikeModel).isEqualTo("Mountain Trail 600")
            assertThat(this.waitingSince).isEqualTo(waitingSince)
        }
    }

    @Test
    fun `skips tasks whose application can no longer be found`() {
        // given: an open task pointing at a vanished application
        every { taskInbox.findOpenClarifications() } returns
            listOf(TaskInboxPort.OpenClarification(id, waitingSince))
        every { repository.findById(id) } returns null
        every { bikePortfolio.findAllByIds(emptyList()) } returns emptyList()

        // when / then: the orphan task is dropped rather than surfacing a half-built row
        assertThat(underTest.pending()).isEmpty()
        verify { bikePortfolio.findAllByIds(emptyList()) }
    }

    @Test
    fun `returns nothing when no clarification is open`() {
        // given: an empty task list
        every { taskInbox.findOpenClarifications() } returns emptyList()
        every { bikePortfolio.findAllByIds(emptyList()) } returns emptyList()

        // when / then: the inbox is empty
        assertThat(underTest.pending()).isEmpty()
    }
}
