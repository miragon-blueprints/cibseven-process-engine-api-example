package io.miragon.blueprint.adapter.inbound.rest;

import io.miragon.blueprint.application.port.inbound.WithdrawApplicationUseCase;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bike-leasing")
public class WithdrawApplicationController {

    private final WithdrawApplicationUseCase useCase;

    public WithdrawApplicationController(WithdrawApplicationUseCase useCase) {
        this.useCase = useCase;
    }

    @Operation(operationId = "withdrawApplication")
    @PostMapping("/{applicationId}/withdraw")
    public ResponseEntity<Void> withdraw(@PathVariable String applicationId) {
        useCase.withdraw(ApplicationId.of(applicationId));
        return ResponseEntity.accepted().build();
    }
}
