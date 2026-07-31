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

class ReportHandoverServiceTest {

    private val process = mockk<LeasingProcess>()
    private val underTest = ReportHandoverService(process = process)

    @Test
    fun `reportHandover correlates the handover-reported message`() {

        // given: an application id
        val id = ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))
        every { process.correlateHandoverReported(id) } just Runs

        // when: the handover is reported
        underTest.reportHandover(id)

        // then: the handover-reported message is correlated for that application
        verify { process.correlateHandoverReported(id) }
        confirmVerified(process)
    }
}
