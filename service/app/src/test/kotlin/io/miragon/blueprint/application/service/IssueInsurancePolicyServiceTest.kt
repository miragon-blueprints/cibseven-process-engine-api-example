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

class IssueInsurancePolicyServiceTest {

    private val insurance = mockk<InsurancePort>()
    private val underTest = IssueInsurancePolicyService(insurance = insurance)

    @Test
    fun `issuePolicy delegates to the insurer`() {
        // given: an eligible application
        val application = testLeasingApplication()
        every { insurance.issuePolicy(application.id) } just Runs
        // when: the policy is issued
        underTest.issuePolicy(application.id)
        // then: the insurance out-port binds the policy
        verify { insurance.issuePolicy(application.id) }
        confirmVerified(insurance)
    }
}
