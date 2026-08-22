package io.miragon.blueprint.adapter.outbound.db

import io.miragon.blueprint.domain.bike.OrderId
import io.miragon.blueprint.domain.leasing.ContractId
import io.miragon.blueprint.domain.leasing.LeasingStatus
import io.miragon.blueprint.domain.leasing.testLeasingApplication
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LeasingApplicationEntityMapperTest {

    @Test
    fun `toEntity copies every field including the nullable order and contract ids`() {
        // given: a fully populated application carrying an order and a contract
        val application = testLeasingApplication(
            status = LeasingStatus.ACTIVE,
            orderId = OrderId("ORDER-1"),
            contractId = ContractId("CONTRACT-1"),
        )

        // when: it is mapped to its persistence entity
        val entity = LeasingApplicationEntityMapper.toEntity(application)

        // then: each field is carried over unchanged
        assertThat(entity.applicationId).isEqualTo(application.id.value)
        assertThat(entity.customerName).isEqualTo(application.customerName.value)
        assertThat(entity.email).isEqualTo(application.email.value)
        assertThat(entity.age).isEqualTo(application.age)
        assertThat(entity.monthlyNetIncome).isEqualTo(application.monthlyNetIncome)
        assertThat(entity.bikeId).isEqualTo(application.bikeId.value)
        assertThat(entity.status).isEqualTo(application.status)
        assertThat(entity.createdAt).isEqualTo(application.createdAt)
        assertThat(entity.orderId).isEqualTo("ORDER-1")
        assertThat(entity.contractId).isEqualTo("CONTRACT-1")
    }

    @Test
    fun `toDomain reconstructs every field including the nullable order and contract ids`() {
        // given: an entity carrying an order and a contract
        val entity = LeasingApplicationEntityMapper.toEntity(
            testLeasingApplication(
                status = LeasingStatus.ACTIVE,
                orderId = OrderId("ORDER-1"),
                contractId = ContractId("CONTRACT-1"),
            ),
        )

        // when: it is mapped back to the domain
        val application = LeasingApplicationEntityMapper.toDomain(entity)

        // then: each field is reconstructed unchanged
        assertThat(application.id.value).isEqualTo(entity.applicationId)
        assertThat(application.customerName.value).isEqualTo(entity.customerName)
        assertThat(application.email.value).isEqualTo(entity.email)
        assertThat(application.age).isEqualTo(entity.age)
        assertThat(application.monthlyNetIncome).isEqualTo(entity.monthlyNetIncome)
        assertThat(application.bikeId.value).isEqualTo(entity.bikeId)
        assertThat(application.status).isEqualTo(entity.status)
        assertThat(application.createdAt).isEqualTo(entity.createdAt)
        assertThat(application.orderId).isEqualTo(OrderId("ORDER-1"))
        assertThat(application.contractId).isEqualTo(ContractId("CONTRACT-1"))
    }

    @Test
    fun `toDomain keeps the order and contract ids null when the entity has none`() {
        // given: an entity without an order or a contract
        val entity = LeasingApplicationEntityMapper.toEntity(testLeasingApplication())

        // when: it is mapped back to the domain
        val application = LeasingApplicationEntityMapper.toDomain(entity)

        // then: both optional references stay null
        assertThat(application.orderId).isNull()
        assertThat(application.contractId).isNull()
    }
}
