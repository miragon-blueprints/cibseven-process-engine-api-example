package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.outbound.LeasingProcess
import io.miragon.blueprint.domain.leasing.ApplicationId
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
    private val underTest = WithdrawApplicationService(process = process)

    @Test
    fun `withdraw correlates the application-withdrawn message`() {

        // given: an application id
        val id = ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))
        every { process.correlateApplicationWithdrawn(id) } just Runs

        // when: the application is withdrawn
        underTest.withdraw(id)

        // then: the application-withdrawn message is correlated for that application
        verify { process.correlateApplicationWithdrawn(id) }
        confirmVerified(process)
    }
}
