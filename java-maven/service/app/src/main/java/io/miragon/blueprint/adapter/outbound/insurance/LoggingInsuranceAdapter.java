package io.miragon.blueprint.adapter.outbound.insurance;

import io.miragon.blueprint.application.port.outbound.InsurancePort;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Blueprint insurance adapter — logs instead of calling a real insurer. Swap this for a real
 * integration (bind / revoke a policy) without touching the application layer.
 */
@Component
public class LoggingInsuranceAdapter implements InsurancePort {

    private static final Logger log = LoggerFactory.getLogger(LoggingInsuranceAdapter.class);

    @Override
    public void issuePolicy(ApplicationId id) {
        // A real system would call the insurer here to bind a policy.
        log.info("Issuing insurance policy for application {}", id.value());
    }

    @Override
    public void cancelPolicy(ApplicationId id) {
        // A real system would call the insurer here to revoke the policy.
        log.info("Cancelling insurance policy for application {}", id.value());
    }
}
