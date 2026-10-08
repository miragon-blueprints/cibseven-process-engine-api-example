package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.outbound.ContractPort
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.application.port.outbound.NotificationPort
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
import java.util.UUID

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

    @Test
    fun `sendContract fails for an unknown application`() {

        // given: an application id the repository does not know
        val unknownId = ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174999"))
        every { repository.findById(unknownId) } returns null

        // when / then: the lookup fails before a contract is issued, persisted or announced
        assertThatThrownBy { underTest.sendContract(unknownId) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Unknown application $unknownId")
        verify { repository.findById(unknownId) }
        confirmVerified(repository, contract, notification)
    }
}
