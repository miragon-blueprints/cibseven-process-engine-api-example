package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.domain.leasing.ApplicationInvalidException
import io.miragon.blueprint.domain.leasing.testLeasingApplication
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ValidateApplicationServiceTest {

    private val repository = mockk<LeasingApplicationRepository>()
    private val underTest = ValidateApplicationService(repository = repository)

    @Test
    fun `validate loads a well-formed application without error`() {

        // given: a valid, solvent application in the repository
        val application = testLeasingApplication()
        every { repository.findById(application.id) } returns application

        // when: the application is validated
        underTest.validate(application.id)

        // then: the application was loaded and accepted
        verify { repository.findById(application.id) }
        confirmVerified(repository)
    }

    @Test
    fun `validate rejects an application without income`() {

        // given: an application with zero monthly net income
        val application = testLeasingApplication(monthlyNetIncome = 0.0)
        every { repository.findById(application.id) } returns application

        // when / then: validation surfaces the application as invalid
        assertThatThrownBy { underTest.validate(application.id) }
            .isInstanceOf(ApplicationInvalidException::class.java)
    }
}
