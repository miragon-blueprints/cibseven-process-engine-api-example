package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.inbound.WithdrawApplicationUseCase
import io.miragon.blueprint.application.port.outbound.LeasingProcess
import io.miragon.blueprint.domain.leasing.ApplicationId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class WithdrawApplicationService(
    private val process: LeasingProcess,
) : WithdrawApplicationUseCase {

    override fun withdraw(id: ApplicationId) {
        process.correlateApplicationWithdrawn(id)
    }
}
