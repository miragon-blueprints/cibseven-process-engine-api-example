package io.miragon.blueprint.adapter.inbound.rest

import com.fasterxml.jackson.annotation.JsonFormat
import io.miragon.blueprint.application.port.inbound.GetPendingClarificationsQuery
import io.miragon.blueprint.domain.leasing.PendingClarification
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

/**
 * `GET /api/tasks/clarify-alternative` — the back-office inbox of applications waiting on the
 * alternative-clarification task.
 *
 * The DTO carries NO task id on purpose. Completing a clarification goes through the domain
 * (`POST /api/bike-leasing/{id}/clarify-alternative`), which correlates by business key — so this
 * read model cannot be used to bypass the domain and complete a task directly. That is exactly the
 * lesson the form-only `clarify-return` task contrasts against.
 */
@RestController
@RequestMapping("/api/tasks")
class GetPendingClarificationsController(
    private val query: GetPendingClarificationsQuery,
) {

    @Operation(operationId = "listPendingClarifications")
    @GetMapping("/clarify-alternative")
    fun pending(): List<PendingClarificationDto> =
        query.pending().map { it.toDto() }

    private fun PendingClarification.toDto() =
        PendingClarificationDto(
            applicationId = applicationId.value.toString(),
            customerName = customerName.value,
            requestedBikeId = requestedBikeId.value,
            requestedBikeModel = requestedBikeModel,
            waitingSince = waitingSince,
        )

    data class PendingClarificationDto(
        val applicationId: String,
        val customerName: String,
        val requestedBikeId: String,
        val requestedBikeModel: String?,
        // ISO-8601 string — see the note in GetLeasingApplicationController.LeasingApplicationDto.
        @field:JsonFormat(shape = JsonFormat.Shape.STRING)
        val waitingSince: LocalDateTime,
    )
}
