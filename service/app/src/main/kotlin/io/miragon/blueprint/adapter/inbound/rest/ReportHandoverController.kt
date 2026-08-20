package io.miragon.blueprint.adapter.inbound.rest

import io.miragon.blueprint.application.port.inbound.ReportHandoverUseCase
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/bike-leasing")
class ReportHandoverController(
    private val useCase: ReportHandoverUseCase,
) {

    @Operation(operationId = "reportHandover")
    @PostMapping("/{applicationId}/report-handover")
    fun reportHandover(@PathVariable applicationId: String): ResponseEntity<Unit> {
        useCase.reportHandover(ApplicationId.of(applicationId))
        return ResponseEntity.accepted().build()
    }
}
