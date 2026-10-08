package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.outbound.InsurancePort
import io.miragon.blueprint.domain.leasing.testLeasingApplication
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class CancelInsurancePolicyServiceTest {

    private val insurance = mockk<InsurancePort>()
    private val underTest = CancelInsurancePolicyService(insurance = insurance)

    @Test
    fun `cancelPolicy delegates the compensation to the insurer`() {
        // given: an application whose policy must be revoked
        val application = testLeasingApplication()
        every { insurance.cancelPolicy(application.id) } just Runs
        // when: the policy is cancelled
        underTest.cancelPolicy(application.id)
        // then: the insurance out-port revokes the policy
        verify { insurance.cancelPolicy(application.id) }
        confirmVerified(insurance)
    }
}
