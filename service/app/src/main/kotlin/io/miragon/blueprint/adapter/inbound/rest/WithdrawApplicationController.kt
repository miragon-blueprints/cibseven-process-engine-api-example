package io.miragon.blueprint.adapter.inbound.rest

import io.miragon.blueprint.application.port.inbound.WithdrawApplicationUseCase
import io.miragon.blueprint.domain.leasing.ApplicationId
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/bike-leasing")
class WithdrawApplicationController(
    private val useCase: WithdrawApplicationUseCase,
) {

    @PostMapping("/{applicationId}/withdraw")
    fun withdraw(@PathVariable applicationId: String): ResponseEntity<Unit> {
        useCase.withdraw(ApplicationId.of(applicationId))
        return ResponseEntity.accepted().build()
    }
}
