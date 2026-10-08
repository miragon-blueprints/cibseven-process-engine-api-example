package io.miragon.blueprint.domain.leasing;

import static io.miragon.blueprint.domain.leasing.TestObjectBuilder.testLeasingApplication;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.miragon.blueprint.domain.bike.BikeId;
import io.miragon.blueprint.domain.bike.OrderId;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LeasingApplicationTest {

    @Test
    void exposes_the_applicants_age_and_monthly_net_income() {
        // given: an application built with a specific age and income
        LeasingApplication application = testLeasingApplication().age(40).monthlyNetIncome(4200.0).build();
        // then: both fields are exposed unchanged
        assertThat(application.age()).isEqualTo(40);
        assertThat(application.monthlyNetIncome()).isEqualTo(4200.0);
    }

    @Test
    void documentOrder_attaches_the_order_id_and_moves_to_ORDERED() {
        // given: a received application
        LeasingApplication application = testLeasingApplication().status(LeasingStatus.RECEIVED).build();
        // when: a bike order is attached
        LeasingApplication ordered = application.documentOrder(new OrderId("ORDER-1"));
        // then: the order id is set and the status is ORDERED
        assertThat(ordered).isEqualTo(
            testLeasingApplication().orderId(new OrderId("ORDER-1")).status(LeasingStatus.ORDERED).build());
    }

    @Test
    void selectAlternative_swaps_in_the_newly_chosen_bike() {
        // given: an application whose requested bike was unavailable
        LeasingApplication application = testLeasingApplication().bikeId(new BikeId("BIKE-900")).build();
        // when: the customer accepts an alternative bike
        LeasingApplication updated = application.selectAlternative(new BikeId("BIKE-ALT"));
        // then: the chosen bike is recorded
        assertThat(updated.bikeId()).isEqualTo(new BikeId("BIKE-ALT"));
        assertThat(updated).isEqualTo(testLeasingApplication().bikeId(new BikeId("BIKE-ALT")).build());
    }

    @Test
    void withContract_records_the_issued_contract() {
        // given: an application without a contract yet
        LeasingApplication application = testLeasingApplication().build();
        // when: the contract system issues a contract
        LeasingApplication updated = application.withContract(new ContractId("CONTRACT-1"));
        // then: the contract id is recorded
        assertThat(updated.contractId()).isEqualTo(new ContractId("CONTRACT-1"));
        assertThat(updated).isEqualTo(testLeasingApplication().contractId(new ContractId("CONTRACT-1")).build());
    }

    @Test
    void reject_changes_the_status_to_REJECTED() {
        // given: a received application
        LeasingApplication application = testLeasingApplication().build();
        // when: it is rejected
        LeasingApplication rejected = application.reject();
        // then: the status is REJECTED
        assertThat(rejected.status()).isEqualTo(LeasingStatus.REJECTED);
        assertThat(rejected).isEqualTo(testLeasingApplication().status(LeasingStatus.REJECTED).build());
    }

    @Test
    void withdraw_moves_the_application_to_WITHDRAWN() {
        // given: a handed-over application
        LeasingApplication application = testLeasingApplication().status(LeasingStatus.HANDED_OVER).build();
        // when: the customer withdraws
        LeasingApplication withdrawn = application.withdraw();
        // then: the status is WITHDRAWN
        assertThat(withdrawn.status()).isEqualTo(LeasingStatus.WITHDRAWN);
        assertThat(withdrawn).isEqualTo(testLeasingApplication().status(LeasingStatus.WITHDRAWN).build());
    }

    @Test
    void reportHandover_moves_the_application_to_HANDED_OVER() {
        // given: an ordered application
        LeasingApplication application = testLeasingApplication().status(LeasingStatus.ORDERED).build();
        // when: the handover is reported
        LeasingApplication handedOver = application.reportHandover();
        // then: the status is HANDED_OVER
        assertThat(handedOver.status()).isEqualTo(LeasingStatus.HANDED_OVER);
        assertThat(handedOver).isEqualTo(testLeasingApplication().status(LeasingStatus.HANDED_OVER).build());
    }

    @Test
    void activate_moves_the_application_to_ACTIVE() {
        // given: a handed-over application
        LeasingApplication application = testLeasingApplication().status(LeasingStatus.HANDED_OVER).build();
        // when: the leasing is activated
        LeasingApplication active = application.activate();
        // then: the status is ACTIVE
        assertThat(active.status()).isEqualTo(LeasingStatus.ACTIVE);
        assertThat(active).isEqualTo(testLeasingApplication().status(LeasingStatus.ACTIVE).build());
    }

    @Test
    void receive_rejects_an_application_whose_monthly_net_income_is_zero() {
        // when / then: an application without income cannot be received
        assertThatThrownBy(() -> receiveApplication(0.0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Monthly net income must be greater than zero");
    }

    @Test
    void receive_accepts_the_smallest_positive_monthly_net_income_as_RECEIVED() {
        // when: an application with a minimal income is received
        LeasingApplication application = receiveApplication(0.01);
        // then: it starts its lifecycle as RECEIVED
        assertThat(application.status()).isEqualTo(LeasingStatus.RECEIVED);
        assertThat(application.monthlyNetIncome()).isEqualTo(0.01);
    }

    @Test
    void receive_creates_a_RECEIVED_application_without_order_and_contract() {
        // given: the data of a freshly submitted leasing request
        ApplicationId id = new ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        // when: the application is received
        LeasingApplication received = LeasingApplication.receive(
            id,
            new CustomerName("John Doe"),
            new Email("john.doe@test.com"),
            35,
            3500.0,
            new BikeId("BIKE-900"),
            createdAt);
        // then: it is RECEIVED, carries the submitted data and has neither order nor contract yet
        assertThat(received).isEqualTo(new LeasingApplication(
            id,
            new CustomerName("John Doe"),
            new Email("john.doe@test.com"),
            35,
            3500.0,
            new BikeId("BIKE-900"),
            LeasingStatus.RECEIVED,
            createdAt,
            null,
            null));
    }

    @Test
    void cancel_moves_the_application_to_CANCELLED() {
        // given: a withdrawn application
        LeasingApplication application = testLeasingApplication().status(LeasingStatus.WITHDRAWN).build();
        // when: the compensation has completed
        LeasingApplication cancelled = application.cancel();
        // then: the status is CANCELLED
        assertThat(cancelled.status()).isEqualTo(LeasingStatus.CANCELLED);
        assertThat(cancelled).isEqualTo(testLeasingApplication().status(LeasingStatus.CANCELLED).build());
    }

    private LeasingApplication receiveApplication(double monthlyNetIncome) {
        return LeasingApplication.receive(
            ApplicationId.newId(),
            new CustomerName("John Doe"),
            new Email("john.doe@test.com"),
            35,
            monthlyNetIncome,
            new BikeId("BIKE-900"),
            LocalDateTime.parse("2024-01-15T10:30:00"));
    }
}
