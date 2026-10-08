package io.miragon.blueprint.domain.leasing

import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.bike.OrderId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class LeasingApplicationTest {

    @Test
    fun `exposes the applicant's age and monthly net income`() {
        // given: an application built with a specific age and income
        val application = testLeasingApplication(age = 40, monthlyNetIncome = 4200.0)
        // then: both fields are exposed unchanged
        assertThat(application.age).isEqualTo(40)
        assertThat(application.monthlyNetIncome).isEqualTo(4200.0)
    }

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
    fun `withdraw moves the application to WITHDRAWN`() {
        // given: a handed-over application
        val application = testLeasingApplication(status = LeasingStatus.HANDED_OVER)
        // when: the customer withdraws
        val withdrawn = application.withdraw()
        // then: the status is WITHDRAWN
        assertThat(withdrawn.status).isEqualTo(LeasingStatus.WITHDRAWN)
    }

    @Test
    fun `reportHandover moves the application to HANDED_OVER`() {
        // given: an ordered application
        val application = testLeasingApplication(status = LeasingStatus.ORDERED)
        // when: the handover is reported
        val handedOver = application.reportHandover()
        // then: the status is HANDED_OVER
        assertThat(handedOver.status).isEqualTo(LeasingStatus.HANDED_OVER)
    }

    @Test
    fun `activate moves the application to ACTIVE`() {
        // given: a handed-over application
        val application = testLeasingApplication(status = LeasingStatus.HANDED_OVER)
        // when: the leasing is activated
        val active = application.activate()
        // then: the status is ACTIVE
        assertThat(active.status).isEqualTo(LeasingStatus.ACTIVE)
    }

    @Test
    fun `validate fails when the monthly net income is zero`() {
        // given: an application without income
        val application = testLeasingApplication(monthlyNetIncome = 0.0)
        // when / then: validation reports the application as invalid
        assertThatThrownBy { application.validate() }.isInstanceOf(ApplicationInvalidException::class.java)
    }
}
