package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.application.port.outbound.NotificationPort
import io.miragon.blueprint.domain.leasing.testLeasingApplication
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class SendCancellationConfirmationServiceTest {

    private val repository = mockk<LeasingApplicationRepository>()
    private val notification = mockk<NotificationPort>()
    private val underTest = SendCancellationConfirmationService(repository = repository, notification = notification)

    @Test
    fun `sendCancellationConfirmation loads the application and confirms the cancellation`() {

        // given: an application in the repository
        val application = testLeasingApplication()
        every { repository.findById(application.id) } returns application
        every { notification.send(any(), application) } just Runs

        // when: the cancellation confirmation is sent
        underTest.sendCancellationConfirmation(application.id)

        // then: the application is loaded and the customer is informed
        verify { repository.findById(application.id) }
        verify { notification.send(any(), application) }
        confirmVerified(repository, notification)
    }
}
