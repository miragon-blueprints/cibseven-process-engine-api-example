package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.LeasingStatus
import io.miragon.blueprint.domain.leasing.testLeasingApplication
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.UUID

class ActivateLeasingServiceTest {

    private val repository = mockk<LeasingApplicationRepository>()
    private val underTest = ActivateLeasingService(repository = repository)

    @Test
    fun `activate loads the application, activates it, and persists the ACTIVE status`() {

        // given: a handed-over application
        val id = ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))
        val application = testLeasingApplication(id = id, status = LeasingStatus.HANDED_OVER)
        every { repository.findById(id) } returns application
        every { repository.save(any()) } answers { firstArg() }

        // when: the leasing is activated
        underTest.activate(id)

        // then: the application is persisted with ACTIVE
        verify { repository.findById(id) }
        verify { repository.save(match { it.status == LeasingStatus.ACTIVE }) }
        confirmVerified(repository)
    }

    @Test
    fun `activate fails for an unknown application`() {

        // given: an id the repository cannot resolve
        val unknownId = ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174999"))
        every { repository.findById(unknownId) } returns null

        // when / then: activation fails with the unknown-application message and nothing is persisted
        assertThatThrownBy { underTest.activate(unknownId) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Unknown application $unknownId")
        verify { repository.findById(unknownId) }
        confirmVerified(repository)
    }
}
