package io.miragon.blueprint.application.service;

import static io.miragon.blueprint.domain.leasing.TestObjectBuilder.testLeasingApplication;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import io.miragon.blueprint.application.port.outbound.InsurancePort;
import io.miragon.blueprint.domain.leasing.LeasingApplication;
import org.junit.jupiter.api.Test;

class CancelInsurancePolicyServiceTest {

    private final InsurancePort insurance = mock(InsurancePort.class);
    private final CancelInsurancePolicyService underTest = new CancelInsurancePolicyService(insurance);

    @Test
    void cancelPolicy_delegates_the_compensation_to_the_insurer() {
        // given: an application whose policy must be revoked
        LeasingApplication application = testLeasingApplication().build();
        // when: the policy is cancelled
        underTest.cancelPolicy(application.id());
        // then: the insurance out-port revokes the policy
        verify(insurance).cancelPolicy(application.id());
        verifyNoMoreInteractions(insurance);
    }
}
