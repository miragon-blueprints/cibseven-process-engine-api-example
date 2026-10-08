package io.miragon.blueprint.adapter.outbound.db;

import static io.miragon.blueprint.domain.leasing.TestObjectBuilder.testLeasingApplication;
import static org.assertj.core.api.Assertions.assertThat;

import io.miragon.blueprint.domain.bike.OrderId;
import io.miragon.blueprint.domain.leasing.ContractId;
import io.miragon.blueprint.domain.leasing.LeasingApplication;
import io.miragon.blueprint.domain.leasing.LeasingStatus;
import org.junit.jupiter.api.Test;

class LeasingApplicationEntityMapperTest {

    @Test
    void toEntity_copies_every_field_including_the_nullable_order_and_contract_ids() {
        // given: a fully populated application carrying an order and a contract
        LeasingApplication application = testLeasingApplication()
                .status(LeasingStatus.ACTIVE)
                .orderId(new OrderId("ORDER-1"))
                .contractId(new ContractId("CONTRACT-1"))
                .build();

        // when: it is mapped to its persistence entity
        LeasingApplicationEntity entity = LeasingApplicationEntityMapper.toEntity(application);

        // then: each field is carried over unchanged
        assertThat(entity.getApplicationId()).isEqualTo(application.id().value());
        assertThat(entity.getCustomerName()).isEqualTo(application.customerName().value());
        assertThat(entity.getEmail()).isEqualTo(application.email().value());
        assertThat(entity.getAge()).isEqualTo(application.age());
        assertThat(entity.getMonthlyNetIncome()).isEqualTo(application.monthlyNetIncome());
        assertThat(entity.getBikeId()).isEqualTo(application.bikeId().value());
        assertThat(entity.getStatus()).isEqualTo(application.status());
        assertThat(entity.getCreatedAt()).isEqualTo(application.createdAt());
        assertThat(entity.getOrderId()).isEqualTo("ORDER-1");
        assertThat(entity.getContractId()).isEqualTo("CONTRACT-1");
    }

    @Test
    void toDomain_reconstructs_every_field_including_the_nullable_order_and_contract_ids() {
        // given: an entity carrying an order and a contract
        LeasingApplicationEntity entity = LeasingApplicationEntityMapper.toEntity(
                testLeasingApplication()
                        .status(LeasingStatus.ACTIVE)
                        .orderId(new OrderId("ORDER-1"))
                        .contractId(new ContractId("CONTRACT-1"))
                        .build());

        // when: it is mapped back to the domain
        LeasingApplication application = LeasingApplicationEntityMapper.toDomain(entity);

        // then: each field is reconstructed unchanged
        assertThat(application.id().value()).isEqualTo(entity.getApplicationId());
        assertThat(application.customerName().value()).isEqualTo(entity.getCustomerName());
        assertThat(application.email().value()).isEqualTo(entity.getEmail());
        assertThat(application.age()).isEqualTo(entity.getAge());
        assertThat(application.monthlyNetIncome()).isEqualTo(entity.getMonthlyNetIncome());
        assertThat(application.bikeId().value()).isEqualTo(entity.getBikeId());
        assertThat(application.status()).isEqualTo(entity.getStatus());
        assertThat(application.createdAt()).isEqualTo(entity.getCreatedAt());
        assertThat(application.orderId()).isEqualTo(new OrderId("ORDER-1"));
        assertThat(application.contractId()).isEqualTo(new ContractId("CONTRACT-1"));
    }

    @Test
    void toDomain_keeps_the_order_and_contract_ids_null_when_the_entity_has_none() {
        // given: an entity without an order or a contract
        LeasingApplicationEntity entity = LeasingApplicationEntityMapper.toEntity(testLeasingApplication().build());

        // when: it is mapped back to the domain
        LeasingApplication application = LeasingApplicationEntityMapper.toDomain(entity);

        // then: both optional references stay null
        assertThat(application.orderId()).isNull();
        assertThat(application.contractId()).isNull();
    }
}
