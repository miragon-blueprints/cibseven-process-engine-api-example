package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.outbound.ContractPort
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.domain.leasing.ContractId
import io.miragon.blueprint.domain.leasing.testLeasingApplication
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
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
}
