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
import org.junit.jupiter.api.Test
import java.util.UUID

class WithdrawApplicationServiceTest {

    private val process = mockk<LeasingProcess>()
    private val repository = mockk<LeasingApplicationRepository>()
    private val underTest = WithdrawApplicationService(process = process, repository = repository)

    @Test
    fun `withdraw correlates the message and persists the WITHDRAWN status`() {

        // given: a handed-over application
        val id = ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))
        val application = testLeasingApplication(id = id, status = LeasingStatus.HANDED_OVER)
        every { process.correlateApplicationWithdrawn(id) } just Runs
        every { repository.findById(id) } returns application
        every { repository.save(any()) } answers { firstArg() }

        // when: the application is withdrawn
        underTest.withdraw(id)

        // then: the message is correlated and the application is persisted with WITHDRAWN
        verify { process.correlateApplicationWithdrawn(id) }
        verify { repository.findById(id) }
        verify { repository.save(match { it.status == LeasingStatus.WITHDRAWN }) }
        confirmVerified(process, repository)
    }

    @Test
    fun `withdraw does not persist when correlation fails`() {

        // given: a correlation that throws
        val id = ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174001"))
        every { process.correlateApplicationWithdrawn(id) } throws RuntimeException("no token")

        // when / then: the exception propagates without touching the repository
        try {
            underTest.withdraw(id)
        } catch (_: RuntimeException) {}

        verify { process.correlateApplicationWithdrawn(id) }
        verify(exactly = 0) { repository.findById(any()) }
        verify(exactly = 0) { repository.save(any()) }
        confirmVerified(process, repository)
    }
}
