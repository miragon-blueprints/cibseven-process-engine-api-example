package io.miragon.blueprint.adapter.inbound.rest;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.miragon.blueprint.application.port.inbound.SelectAlternativeUseCase;
import io.miragon.blueprint.domain.bike.BikeId;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * External completion of the {@code Clarify alternative with customer} user task — the counterpart to a
 * human completing the deployed Camunda Form in the CIB seven Tasklist.
 */
@RestController
@RequestMapping("/api/bike-leasing")
public class SelectAlternativeController {

    private final SelectAlternativeUseCase useCase;

    public SelectAlternativeController(SelectAlternativeUseCase useCase) {
        this.useCase = useCase;
    }

    @Operation(operationId = "selectAlternative")
    @PostMapping("/{applicationId}/clarify-alternative")
    public ResponseEntity<Void> clarifyAlternative(
            @PathVariable String applicationId,
            @RequestBody AlternativeDecisionInput input) {
        useCase.selectAlternative(
                new SelectAlternativeUseCase.Command(
                        ApplicationId.of(applicationId),
                        input.alternativeFound(),
                        input.bikeId() == null ? null : new BikeId(input.bikeId()),
                        input.bikeModel()));
        return ResponseEntity.accepted().build();
    }

    /**
     * @param bikeId    the newly chosen bike; {@code null} (or absent) when no alternative was found
     * @param bikeModel the newly chosen bike's model; {@code null} (or absent) when no alternative was found
     */
    public record AlternativeDecisionInput(
            @Schema(requiredMode = REQUIRED) boolean alternativeFound,
            @Schema(nullable = true) String bikeId,
            @Schema(nullable = true) String bikeModel) {
    }
}
