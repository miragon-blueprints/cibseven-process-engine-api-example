package io.miragon.blueprint.domain.leasing

import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.bike.OrderId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class LeasingApplicationTest {

    @Test
    fun `documentOrder attaches the order id and moves to ORDERED`() {
        // given: a received application
        val application = testLeasingApplication(status = LeasingStatus.RECEIVED)
        // when: a bike order is attached
        val ordered = application.documentOrder(OrderId("ORDER-1"))
        // then: the order id is set and the status is ORDERED
        assertThat(ordered).isEqualTo(application.copy(orderId = OrderId("ORDER-1"), status = LeasingStatus.ORDERED))
    }

    @Test
    fun `selectAlternative swaps in the newly chosen bike`() {
        // given: an application whose requested bike was unavailable
        val application = testLeasingApplication(bikeId = BikeId("BIKE-900"))
        // when: the customer accepts an alternative bike
        val updated = application.selectAlternative(BikeId("BIKE-ALT"))
        // then: the chosen bike is recorded
        assertThat(updated.bikeId).isEqualTo(BikeId("BIKE-ALT"))
    }

    @Test
    fun `withContract records the issued contract`() {
        // given: an application without a contract yet
        val application = testLeasingApplication()
        // when: the contract system issues a contract
        val updated = application.withContract(ContractId("CONTRACT-1"))
        // then: the contract id is recorded
        assertThat(updated.contractId).isEqualTo(ContractId("CONTRACT-1"))
    }

    @Test
    fun `reject changes the status to REJECTED`() {
        // given: a received application
        val application = testLeasingApplication()
        // when: it is rejected
        val rejected = application.reject()
        // then: the status is REJECTED
        assertThat(rejected.status).isEqualTo(LeasingStatus.REJECTED)
    }

    @Test
    fun `validate fails when the monthly net income is zero`() {
        // given: an application without income
        val application = testLeasingApplication(monthlyNetIncome = 0.0)
        // when / then: validation reports the application as invalid
        assertThatThrownBy { application.validate() }.isInstanceOf(ApplicationInvalidException::class.java)
    }
}
