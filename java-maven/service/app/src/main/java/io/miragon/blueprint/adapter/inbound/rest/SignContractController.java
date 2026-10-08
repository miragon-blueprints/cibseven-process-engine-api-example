package io.miragon.blueprint.adapter.inbound.rest;

import io.miragon.blueprint.application.port.inbound.SignContractUseCase;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bike-leasing")
public class SignContractController {

    private final SignContractUseCase useCase;

    public SignContractController(SignContractUseCase useCase) {
        this.useCase = useCase;
    }

    @Operation(operationId = "signContract")
    @PostMapping("/{applicationId}/sign-contract")
    public ResponseEntity<Void> signContract(@PathVariable String applicationId) {
        useCase.signContract(ApplicationId.of(applicationId));
        return ResponseEntity.accepted().build();
    }
}
