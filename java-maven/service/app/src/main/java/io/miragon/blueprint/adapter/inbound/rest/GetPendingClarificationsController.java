package io.miragon.blueprint.adapter.inbound.rest;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.miragon.blueprint.application.port.inbound.GetPendingClarificationsQuery;
import io.miragon.blueprint.domain.leasing.PendingClarification;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /api/tasks/clarify-alternative} — the back-office inbox of applications waiting on the
 * alternative-clarification task.
 *
 * <p>The DTO carries NO task id on purpose. Completing a clarification goes through the domain
 * ({@code POST /api/bike-leasing/{id}/clarify-alternative}), which correlates by business key — so this
 * read model cannot be used to bypass the domain and complete a task directly. That is exactly the
 * lesson the form-only {@code clarify-return} task contrasts against.
 */
@RestController
@RequestMapping("/api/tasks")
public class GetPendingClarificationsController {

    private final GetPendingClarificationsQuery query;

    public GetPendingClarificationsController(GetPendingClarificationsQuery query) {
        this.query = query;
    }

    @Operation(operationId = "listPendingClarifications")
    @GetMapping("/clarify-alternative")
    public List<PendingClarificationDto> pending() {
        return query.pending().stream()
                .map(GetPendingClarificationsController::toDto)
                .toList();
    }

    private static PendingClarificationDto toDto(PendingClarification clarification) {
        return new PendingClarificationDto(
                clarification.applicationId().value().toString(),
                clarification.customerName().value(),
                clarification.requestedBikeId().value(),
                clarification.requestedBikeModel(),
                clarification.waitingSince());
    }

    /**
     * @param requestedBikeModel the requested bike's model; {@code null} if it is not in the portfolio
     */
    public record PendingClarificationDto(
            @Schema(requiredMode = REQUIRED) String applicationId,
            @Schema(requiredMode = REQUIRED) String customerName,
            @Schema(requiredMode = REQUIRED) String requestedBikeId,
            @Schema(nullable = true) String requestedBikeModel,
            // ISO-8601 string — see the note in GetLeasingApplicationController.LeasingApplicationDto.
            @JsonFormat(shape = JsonFormat.Shape.STRING)
            @Schema(requiredMode = REQUIRED) LocalDateTime waitingSince) {
    }
}
