package io.miragon.blueprint.adapter.outbound.db;

import io.miragon.blueprint.domain.leasing.LeasingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity(name = "leasing_application")
public class LeasingApplicationEntity {

    @Id
    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "age", nullable = false)
    private int age;

    @Column(name = "monthly_net_income", nullable = false)
    private double monthlyNetIncome;

    @Column(name = "bike_id", nullable = false)
    private String bikeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LeasingStatus status;

    @Column(name = "order_id")
    private String orderId;

    @Column(name = "contract_id")
    private String contractId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** For JPA only. */
    protected LeasingApplicationEntity() {
    }

    /**
     * @param orderId    the dealer order; {@code null} until a bike was ordered
     * @param contractId the issued contract; {@code null} until a contract was issued
     */
    public LeasingApplicationEntity(
            UUID applicationId,
            String customerName,
            String email,
            int age,
            double monthlyNetIncome,
            String bikeId,
            LeasingStatus status,
            String orderId,
            String contractId,
            LocalDateTime createdAt) {
        this.applicationId = applicationId;
        this.customerName = customerName;
        this.email = email;
        this.age = age;
        this.monthlyNetIncome = monthlyNetIncome;
        this.bikeId = bikeId;
        this.status = status;
        this.orderId = orderId;
        this.contractId = contractId;
        this.createdAt = createdAt;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getEmail() {
        return email;
    }

    public int getAge() {
        return age;
    }

    public double getMonthlyNetIncome() {
        return monthlyNetIncome;
    }

    public String getBikeId() {
        return bikeId;
    }

    public LeasingStatus getStatus() {
        return status;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getContractId() {
        return contractId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
