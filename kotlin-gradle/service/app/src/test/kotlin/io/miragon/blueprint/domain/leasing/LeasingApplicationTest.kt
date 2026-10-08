package io.miragon.blueprint.domain.leasing

import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.bike.OrderId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

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

    @Test
    fun `receive creates a RECEIVED application without order and contract`() {
        // given: the data of a freshly submitted leasing request
        val id = ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))
        val createdAt = LocalDateTime.of(2024, 1, 15, 10, 30, 0)
        // when: the application is received
        val received = LeasingApplication.receive(
            id = id,
            customerName = CustomerName("John Doe"),
            email = Email("john.doe@test.com"),
            age = 35,
            monthlyNetIncome = 3500.0,
            bikeId = BikeId("BIKE-900"),
            createdAt = createdAt,
        )
        // then: it is RECEIVED, carries the submitted data and has neither order nor contract yet
        assertThat(received).isEqualTo(
            LeasingApplication(
                id = id,
                customerName = CustomerName("John Doe"),
                email = Email("john.doe@test.com"),
                age = 35,
                monthlyNetIncome = 3500.0,
                bikeId = BikeId("BIKE-900"),
                status = LeasingStatus.RECEIVED,
                createdAt = createdAt,
                orderId = null,
                contractId = null,
            ),
        )
    }

    @Test
    fun `cancel moves the application to CANCELLED`() {
        // given: a withdrawn application
        val application = testLeasingApplication(status = LeasingStatus.WITHDRAWN)
        // when: the compensation has completed
        val cancelled = application.cancel()
        // then: the status is CANCELLED
        assertThat(cancelled).isEqualTo(application.copy(status = LeasingStatus.CANCELLED))
    }

    @Test
    fun `validate returns the unchanged application when the monthly net income is positive`() {
        // given: a solvent application
        val application = testLeasingApplication(monthlyNetIncome = 3500.0)
        // when: it is validated
        val validated = application.validate()
        // then: the very same application is returned
        assertThat(validated).isSameAs(application)
    }
}
