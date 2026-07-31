package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.inbound.ReportHandoverUseCase
import io.miragon.blueprint.application.port.outbound.LeasingProcess
import io.miragon.blueprint.domain.leasing.ApplicationId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ReportHandoverService(
    private val process: LeasingProcess,
) : ReportHandoverUseCase {

    override fun reportHandover(id: ApplicationId) {
        process.correlateHandoverReported(id)
    }
}
