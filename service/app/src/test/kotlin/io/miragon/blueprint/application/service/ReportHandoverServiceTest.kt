package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.application.port.outbound.LeasingProcess
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.LeasingStatus
import io.miragon.blueprint.domain.leasing.testLeasingApplication
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class ReportHandoverServiceTest {

    private val process = mockk<LeasingProcess>()
    private val repository = mockk<LeasingApplicationRepository>()
    private val underTest = ReportHandoverService(process = process, repository = repository)

    @Test
    fun `reportHandover correlates the message and persists the HANDED_OVER status`() {

        // given: an ordered application
        val id = ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))
        val application = testLeasingApplication(id = id, status = LeasingStatus.ORDERED)
        every { process.correlateHandoverReported(id) } just Runs
        every { repository.findById(id) } returns application
        every { repository.save(any()) } answers { firstArg() }

        // when: the handover is reported
        underTest.reportHandover(id)

        // then: the message is correlated and the application is persisted with HANDED_OVER
        verify { process.correlateHandoverReported(id) }
        verify { repository.findById(id) }
        verify { repository.save(match { it.status == LeasingStatus.HANDED_OVER }) }
        confirmVerified(process, repository)
    }

    @Test
    fun `reportHandover does not persist when correlation fails`() {

        // given: a correlation that throws
        val id = ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174001"))
        every { process.correlateHandoverReported(id) } throws RuntimeException("no token")

        // when / then: the exception propagates without touching the repository
        try {
            underTest.reportHandover(id)
        } catch (_: RuntimeException) {}

        verify { process.correlateHandoverReported(id) }
        verify(exactly = 0) { repository.findById(any()) }
        verify(exactly = 0) { repository.save(any()) }
        confirmVerified(process, repository)
    }
}
