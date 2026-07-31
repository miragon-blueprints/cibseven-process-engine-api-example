package io.miragon.blueprint.adapter.inbound.rest

import io.miragon.blueprint.application.port.inbound.SignContractUseCase
import io.miragon.blueprint.domain.leasing.ApplicationId
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/bike-leasing")
class SignContractController(
    private val useCase: SignContractUseCase,
) {

    @PostMapping("/{applicationId}/sign-contract")
    fun signContract(@PathVariable applicationId: String): ResponseEntity<Unit> {
        useCase.signContract(ApplicationId.of(applicationId))
        return ResponseEntity.accepted().build()
    }
}
