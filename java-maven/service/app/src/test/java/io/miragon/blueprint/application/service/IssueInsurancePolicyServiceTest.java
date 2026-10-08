package io.miragon.blueprint.application.service;

import static io.miragon.blueprint.domain.leasing.TestObjectBuilder.testLeasingApplication;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import io.miragon.blueprint.application.port.outbound.InsurancePort;
import io.miragon.blueprint.domain.leasing.LeasingApplication;
import org.junit.jupiter.api.Test;

class IssueInsurancePolicyServiceTest {

    private final InsurancePort insurance = mock(InsurancePort.class);
    private final IssueInsurancePolicyService underTest = new IssueInsurancePolicyService(insurance);

    @Test
    void issuePolicy_delegates_to_the_insurer() {
        // given: an eligible application
        LeasingApplication application = testLeasingApplication().build();
        // when: the policy is issued
        underTest.issuePolicy(application.id());
        // then: the insurance out-port binds the policy
        verify(insurance).issuePolicy(application.id());
        verifyNoMoreInteractions(insurance);
    }
}
