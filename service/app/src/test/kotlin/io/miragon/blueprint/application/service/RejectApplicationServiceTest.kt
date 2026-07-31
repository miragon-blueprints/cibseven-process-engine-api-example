package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.application.port.outbound.NotificationPort
import io.miragon.blueprint.domain.leasing.LeasingStatus
import io.miragon.blueprint.domain.leasing.testLeasingApplication
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class RejectApplicationServiceTest {

    private val repository = mockk<LeasingApplicationRepository>()
    private val notification = mockk<NotificationPort>()
    private val underTest = RejectApplicationService(repository = repository, notification = notification)

    @Test
    fun `reject notifies the customer and persists the rejected status`() {

        // given: an application in the repository
        val application = testLeasingApplication()
        every { repository.findById(application.id) } returns application
        every { notification.send(any(), application) } just Runs
        every { repository.save(any()) } answers { firstArg() }

        // when: the application is rejected
        underTest.reject(application.id)

        // then: the application is loaded, the customer notified and the application saved as REJECTED
        verify { repository.findById(application.id) }
        verify { notification.send(any(), application) }
        verify { repository.save(match { it.status == LeasingStatus.REJECTED }) }
        confirmVerified(repository, notification)
    }
}
