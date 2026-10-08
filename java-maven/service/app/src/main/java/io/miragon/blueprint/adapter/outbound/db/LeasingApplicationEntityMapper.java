package io.miragon.blueprint.adapter.outbound.db;

import io.miragon.blueprint.domain.bike.BikeId;
import io.miragon.blueprint.domain.bike.OrderId;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import io.miragon.blueprint.domain.leasing.ContractId;
import io.miragon.blueprint.domain.leasing.CustomerName;
import io.miragon.blueprint.domain.leasing.Email;
import io.miragon.blueprint.domain.leasing.LeasingApplication;

public final class LeasingApplicationEntityMapper {

    private LeasingApplicationEntityMapper() {
    }

    public static LeasingApplication toDomain(LeasingApplicationEntity entity) {
        return new LeasingApplication(
                new ApplicationId(entity.getApplicationId()),
                new CustomerName(entity.getCustomerName()),
                new Email(entity.getEmail()),
                entity.getAge(),
                entity.getMonthlyNetIncome(),
                new BikeId(entity.getBikeId()),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getOrderId() == null ? null : new OrderId(entity.getOrderId()),
                entity.getContractId() == null ? null : new ContractId(entity.getContractId()));
    }

    public static LeasingApplicationEntity toEntity(LeasingApplication domain) {
        return new LeasingApplicationEntity(
                domain.id().value(),
                domain.customerName().value(),
                domain.email().value(),
                domain.age(),
                domain.monthlyNetIncome(),
                domain.bikeId().value(),
                domain.status(),
                domain.orderId() == null ? null : domain.orderId().value(),
                domain.contractId() == null ? null : domain.contractId().value(),
                domain.createdAt());
    }
}
