package io.miragon.blueprint.adapter.outbound.insurance

import io.miragon.blueprint.domain.leasing.testLeasingApplication
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Test

class LoggingInsuranceAdapterTest {

    private val underTest = LoggingInsuranceAdapter()

    @Test
    fun `issuePolicy logs without error`() {
        // given: an application / when-then: issuing the policy runs without error
        assertThatCode { underTest.issuePolicy(testLeasingApplication().id) }.doesNotThrowAnyException()
    }

    @Test
    fun `cancelPolicy logs without error`() {
        // given: an application / when-then: cancelling the policy runs without error
        assertThatCode { underTest.cancelPolicy(testLeasingApplication().id) }.doesNotThrowAnyException()
    }
}
