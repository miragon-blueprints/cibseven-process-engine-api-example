package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.outbound.ContractPort
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.ContractId
import io.miragon.blueprint.domain.leasing.testLeasingApplication
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class CancelContractServiceTest {

    private val repository = mockk<LeasingApplicationRepository>()
    private val contract = mockk<ContractPort>()
    private val underTest = CancelContractService(repository = repository, contract = contract)

    @Test
    fun `cancelContract revokes the contract recorded on the application`() {

        // given: an application that already carries an issued contract
        val application = testLeasingApplication(contractId = ContractId("CONTRACT-1"))
        every { repository.findById(application.id) } returns application
        every { contract.revokeContract(any()) } just Runs

        // when: the contract is cancelled as part of the compensation
        underTest.cancelContract(application.id)

        // then: the recorded contract id is revoked in the contract system
        verify { repository.findById(application.id) }
        verify { contract.revokeContract(ContractId("CONTRACT-1")) }
        confirmVerified(repository, contract)
    }

    @Test
    fun `cancelContract fails when the application is unknown`() {

        // given: an id the repository cannot resolve
        val id = ApplicationId.of("123e4567-e89b-12d3-a456-426614174000")
        every { repository.findById(id) } returns null

        // when/then: cancellation fails and no contract is touched
        assertThatThrownBy { underTest.cancelContract(id) }
            .isInstanceOf(IllegalStateException::class.java)
        verify { repository.findById(id) }
        confirmVerified(repository, contract)
    }

    @Test
    fun `cancelContract fails when no contract was issued for the application`() {

        // given: an application that never received a contract
        val application = testLeasingApplication(contractId = null)
        every { repository.findById(application.id) } returns application

        // when/then: cancellation fails and no contract is touched
        assertThatThrownBy { underTest.cancelContract(application.id) }
            .isInstanceOf(IllegalStateException::class.java)
        verify { repository.findById(application.id) }
        confirmVerified(repository, contract)
    }
}
