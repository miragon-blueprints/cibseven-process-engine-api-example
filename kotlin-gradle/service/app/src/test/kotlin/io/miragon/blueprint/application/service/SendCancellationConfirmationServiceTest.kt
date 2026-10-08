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
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SendCancellationConfirmationServiceTest {

    private val repository = mockk<LeasingApplicationRepository>()
    private val notification = mockk<NotificationPort>()
    private val underTest = SendCancellationConfirmationService(repository = repository, notification = notification)

    @Test
    fun `sendCancellationConfirmation confirms to the customer and marks the application cancelled`() {

        // given: an application in the repository
        val application = testLeasingApplication()
        every { repository.findById(application.id) } returns application
        every { notification.send(any(), application) } just Runs
        val saved = slot<io.miragon.blueprint.domain.leasing.LeasingApplication>()
        every { repository.save(capture(saved)) } answers { saved.captured }

        // when: the cancellation confirmation is sent
        underTest.sendCancellationConfirmation(application.id)

        // then: the customer is informed and the application is moved to CANCELLED
        verify { repository.findById(application.id) }
        verify { notification.send(any(), application) }
        verify { repository.save(any()) }
        assertThat(saved.captured.status).isEqualTo(LeasingStatus.CANCELLED)
        confirmVerified(repository, notification)
    }
}
