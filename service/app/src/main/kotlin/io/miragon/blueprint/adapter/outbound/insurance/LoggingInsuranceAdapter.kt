package io.miragon.blueprint.adapter.outbound.insurance

import io.miragon.blueprint.application.port.outbound.InsurancePort
import io.miragon.blueprint.domain.leasing.ApplicationId
import mu.KotlinLogging
import org.springframework.stereotype.Component

/**
 * Blueprint insurance adapter — logs instead of calling a real insurer. Swap this for a real
 * integration (bind / revoke a policy) without touching the application layer.
 */
@Component
class LoggingInsuranceAdapter : InsurancePort {

    private val log = KotlinLogging.logger {}

    override fun issuePolicy(id: ApplicationId) {
        // A real system would call the insurer here to bind a policy.
        log.info { "Issuing insurance policy for application ${id.value}" }
    }

    override fun cancelPolicy(id: ApplicationId) {
        // A real system would call the insurer here to revoke the policy.
        log.info { "Cancelling insurance policy for application ${id.value}" }
    }
}
