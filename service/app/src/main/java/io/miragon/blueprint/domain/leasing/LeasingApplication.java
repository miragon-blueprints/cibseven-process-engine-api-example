package io.miragon.blueprint.domain.leasing;

import io.miragon.blueprint.domain.bike.BikeId;
import io.miragon.blueprint.domain.bike.OrderId;
import java.time.LocalDateTime;

/**
 * Aggregate root of the bike-leasing domain. All state transitions return a copy, so an instance is
 * never mutated in place — the calling service persists the returned copy.
 *
 * @param orderId    the dealer order; {@code null} until a bike was ordered
 * @param contractId the issued contract; {@code null} until a contract was issued
 */
public record LeasingApplication(
        ApplicationId id,
        CustomerName customerName,
        Email email,
        int age,
        double monthlyNetIncome,
        BikeId bikeId,
        LeasingStatus status,
        LocalDateTime createdAt,
        OrderId orderId,
        ContractId contractId) {

    public static LeasingApplication receive(
            ApplicationId id,
            CustomerName customerName,
            Email email,
            int age,
            double monthlyNetIncome,
            BikeId bikeId,
            LocalDateTime createdAt) {
        return new LeasingApplication(
                id,
                customerName,
                email,
                age,
                monthlyNetIncome,
                bikeId,
                LeasingStatus.RECEIVED,
                createdAt,
                null,
                null);
    }

    public LeasingApplication validate() {
        if (monthlyNetIncome <= 0.0) {
            throw new ApplicationInvalidException(id, "monthly net income must be greater than zero");
        }
        return this;
    }

    public LeasingApplication withContract(ContractId contractId) {
        return new LeasingApplication(
                id, customerName, email, age, monthlyNetIncome, bikeId, status, createdAt, orderId, contractId);
    }

    public LeasingApplication documentOrder(OrderId orderId) {
        return new LeasingApplication(
                id, customerName, email, age, monthlyNetIncome, bikeId, LeasingStatus.ORDERED, createdAt, orderId,
                contractId);
    }

    public LeasingApplication selectAlternative(BikeId bikeId) {
        return new LeasingApplication(
                id, customerName, email, age, monthlyNetIncome, bikeId, status, createdAt, orderId, contractId);
    }

    public LeasingApplication reportHandover() {
        return withStatus(LeasingStatus.HANDED_OVER);
    }

    public LeasingApplication activate() {
        return withStatus(LeasingStatus.ACTIVE);
    }

    public LeasingApplication withdraw() {
        return withStatus(LeasingStatus.WITHDRAWN);
    }

    public LeasingApplication reject() {
        return withStatus(LeasingStatus.REJECTED);
    }

    public LeasingApplication cancel() {
        return withStatus(LeasingStatus.CANCELLED);
    }

    private LeasingApplication withStatus(LeasingStatus status) {
        return new LeasingApplication(
                id, customerName, email, age, monthlyNetIncome, bikeId, status, createdAt, orderId, contractId);
    }
}
