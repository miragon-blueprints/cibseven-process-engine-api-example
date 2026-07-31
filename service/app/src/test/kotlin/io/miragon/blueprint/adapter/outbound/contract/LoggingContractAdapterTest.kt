package io.miragon.blueprint.adapter.outbound.contract

import io.miragon.blueprint.domain.leasing.ContractId
import io.miragon.blueprint.domain.leasing.testLeasingApplication
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Test

class LoggingContractAdapterTest {

    private val underTest = LoggingContractAdapter()

    @Test
    fun `issueContract mints a fresh contract id`() {
        // given: an application / when: a contract is issued twice
        val first = underTest.issueContract(testLeasingApplication().id)
        val second = underTest.issueContract(testLeasingApplication().id)
        // then: each issue gets its own reference
        assertThat(first.value).startsWith("CONTRACT-")
        assertThat(first).isNotEqualTo(second)
    }

    @Test
    fun `revokeContract logs without error`() {
        // given: an issued contract / when-then: revoking it runs without error
        assertThatCode { underTest.revokeContract(ContractId("CONTRACT-1")) }.doesNotThrowAnyException()
    }
}
