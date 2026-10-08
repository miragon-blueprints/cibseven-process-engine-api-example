package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.outbound.ContractPort
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.application.port.outbound.NotificationPort
import io.miragon.blueprint.domain.leasing.ContractId
import io.miragon.blueprint.domain.leasing.testLeasingApplication
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class SendContractServiceTest {

    private val repository = mockk<LeasingApplicationRepository>()
    private val contract = mockk<ContractPort>()
    private val notification = mockk<NotificationPort>()
    private val underTest =
        SendContractService(repository = repository, contract = contract, notification = notification)

    @Test
    fun `sendContract issues the contract, records its id on the application and notifies the customer`() {

        // given: an application whose contract the contract system will issue
        val application = testLeasingApplication()
        every { repository.findById(application.id) } returns application
        every { contract.issueContract(application.id) } returns ContractId("CONTRACT-1")
        every { repository.save(any()) } answers { firstArg() }
        every { notification.send(any(), any()) } just Runs

        // when: the contract is sent
        underTest.sendContract(application.id)

        // then: the contract is issued, its id is stored on the application and the customer is asked to sign
        verify { repository.findById(application.id) }
        verify { contract.issueContract(application.id) }
        verify { repository.save(match { it.contractId == ContractId("CONTRACT-1") }) }
        verify { notification.send(any(), application) }
        confirmVerified(repository, contract, notification)
    }
}
