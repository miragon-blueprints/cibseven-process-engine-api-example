package io.miragon.blueprint.adapter.inbound.rest;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.miragon.blueprint.application.port.inbound.GetLeasingApplicationQuery;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import io.miragon.blueprint.domain.leasing.LeasingApplication;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bike-leasing")
public class GetLeasingApplicationController {

    private final GetLeasingApplicationQuery query;

    public GetLeasingApplicationController(GetLeasingApplicationQuery query) {
        this.query = query;
    }

    @Operation(operationId = "getLeasingApplication")
    @GetMapping("/{applicationId}")
    public ResponseEntity<LeasingApplicationDto> byId(@PathVariable String applicationId) {
        return query.byId(ApplicationId.of(applicationId))
                .map(result -> ResponseEntity.ok(toDto(result)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * @param bikeModel  the bike's model; {@code null} if the bike is not in the portfolio
     * @param orderId    the dealer order id; {@code null} until a bike was ordered
     * @param contractId the issued contract id; {@code null} until a contract was issued
     */
    public record LeasingApplicationDto(
            @Schema(requiredMode = REQUIRED) String applicationId,
            @Schema(requiredMode = REQUIRED) String customerName,
            @Schema(requiredMode = REQUIRED) String email,
            @Schema(requiredMode = REQUIRED) int age,
            @Schema(requiredMode = REQUIRED) double monthlyNetIncome,
            @Schema(requiredMode = REQUIRED) String bikeId,
            @Schema(nullable = true) String bikeModel,
            @Schema(requiredMode = REQUIRED) String status,
            @Schema(nullable = true) String orderId,
            @Schema(nullable = true) String contractId,
            // Force ISO-8601 string form: Jackson 3 (SB4) defaults to a numeric array, but the CIB seven
            // webapp serves /api with its own Jackson mapper that ignores our global date-time config, so
            // the format is pinned at the field to keep the payload in sync with the springdoc contract
            // any API consumer relies on.
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            @Schema(requiredMode = REQUIRED) LocalDateTime createdAt) {
    }

    private static LeasingApplicationDto toDto(GetLeasingApplicationQuery.Result result) {
        LeasingApplication application = result.application();
        return new LeasingApplicationDto(
                application.id().value().toString(),
                application.customerName().value(),
                application.email().value(),
                application.age(),
                application.monthlyNetIncome(),
                application.bikeId().value(),
                // resolved from the bike portfolio, not carried on the application
                result.bikeModel(),
                application.status().name(),
                application.orderId() == null ? null : application.orderId().value(),
                application.contractId() == null ? null : application.contractId().value(),
                application.createdAt());
    }
}
