package io.miragon.blueprint.adapter.outbound.notification

import io.miragon.blueprint.domain.leasing.testLeasingApplication
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Test

class LoggingNotificationAdapterTest {

    private val underTest = LoggingNotificationAdapter()

    @Test
    fun `send logs the notification without error`() {

        // given: a subject and an application
        val application = testLeasingApplication()

        // when / then: sending the notification completes without throwing
        assertThatCode { underTest.send("Please review and sign your leasing contract", application) }
            .doesNotThrowAnyException()
    }
}
