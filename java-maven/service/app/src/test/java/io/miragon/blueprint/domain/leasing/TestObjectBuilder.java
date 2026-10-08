package io.miragon.blueprint.domain.leasing;

import io.miragon.blueprint.domain.bike.BikeId;
import io.miragon.blueprint.domain.bike.OrderId;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Shared test builder — start from a valid, solvent application and override only what a test cares about.
 *
 * <p>Usage: {@code testLeasingApplication().id(id).status(LeasingStatus.ACTIVE).build()}. Every setter returns
 * the builder; {@link #build()} can be called repeatedly and always returns a new {@link LeasingApplication}.
 */
public final class TestObjectBuilder {

    private ApplicationId id = new ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
    private CustomerName customerName = new CustomerName("John Doe");
    private Email email = new Email("john.doe@test.com");
    private int age = 35;
    private double monthlyNetIncome = 3500.0;
    private BikeId bikeId = new BikeId("BIKE-900");
    private LeasingStatus status = LeasingStatus.RECEIVED;
    private LocalDateTime createdAt = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
    private OrderId orderId = null;
    private ContractId contractId = null;

    private TestObjectBuilder() {
    }

    /** Starts a builder pre-filled with the defaults of a valid, solvent application in status {@code RECEIVED}. */
    public static TestObjectBuilder testLeasingApplication() {
        return new TestObjectBuilder();
    }

    public TestObjectBuilder id(ApplicationId id) {
        this.id = id;
        return this;
    }

    public TestObjectBuilder customerName(CustomerName customerName) {
        this.customerName = customerName;
        return this;
    }

    public TestObjectBuilder email(Email email) {
        this.email = email;
        return this;
    }

    public TestObjectBuilder age(int age) {
        this.age = age;
        return this;
    }

    public TestObjectBuilder monthlyNetIncome(double monthlyNetIncome) {
        this.monthlyNetIncome = monthlyNetIncome;
        return this;
    }

    public TestObjectBuilder bikeId(BikeId bikeId) {
        this.bikeId = bikeId;
        return this;
    }

    public TestObjectBuilder status(LeasingStatus status) {
        this.status = status;
        return this;
    }

    public TestObjectBuilder createdAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    /** @param orderId the dealer order; {@code null} (the default) for a not-yet-ordered application */
    public TestObjectBuilder orderId(OrderId orderId) {
        this.orderId = orderId;
        return this;
    }

    /** @param contractId the issued contract; {@code null} (the default) when no contract was issued */
    public TestObjectBuilder contractId(ContractId contractId) {
        this.contractId = contractId;
        return this;
    }

    public LeasingApplication build() {
        return new LeasingApplication(
            id,
            customerName,
            email,
            age,
            monthlyNetIncome,
            bikeId,
            status,
            createdAt,
            orderId,
            contractId);
    }
}
