package io.miragon.blueprint.adapter.inbound.rest

import com.fasterxml.jackson.annotation.JsonFormat
import io.miragon.blueprint.application.port.inbound.ListLeasingApplicationsQuery
import io.miragon.blueprint.domain.leasing.LeasingStatus
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

/**
 * `GET /api/bike-leasing?status=&page=&size=` — the customer-portal list. A separate controller from
 * the paging query keeps to the "one inbound port per controller" rule the architecture tests enforce.
 */
@RestController
@RequestMapping("/api/bike-leasing")
class ListLeasingApplicationsController(
    private val query: ListLeasingApplicationsQuery,
) {

    @Operation(operationId = "listLeasingApplications")
    @GetMapping
    fun list(
        @RequestParam(required = false) status: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): LeasingApplicationPageDto {
        val filter = ListLeasingApplicationsQuery.Filter(
            status = status?.let { parseStatus(it) },
            page = page,
            size = size,
        )
        return query.list(filter).toDto()
    }

    private fun parseStatus(raw: String): LeasingStatus =
        try {
            LeasingStatus.valueOf(raw.uppercase())
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("unknown status '$raw'; expected one of ${LeasingStatus.entries.joinToString()}", e)
        }

    private fun ListLeasingApplicationsQuery.Page.toDto() =
        LeasingApplicationPageDto(
            items = items.map { it.toDto() },
            page = page,
            size = size,
            totalElements = totalElements,
            totalPages = totalPages,
        )

    private fun ListLeasingApplicationsQuery.Item.toDto() =
        LeasingApplicationSummaryDto(
            applicationId = applicationId.value.toString(),
            customerName = customerName.value,
            bikeId = bikeId.value,
            bikeModel = bikeModel,
            status = status.name,
            createdAt = createdAt,
        )

    data class LeasingApplicationPageDto(
        val items: List<LeasingApplicationSummaryDto>,
        val page: Int,
        val size: Int,
        val totalElements: Long,
        val totalPages: Int,
    )

    data class LeasingApplicationSummaryDto(
        val applicationId: String,
        val customerName: String,
        val bikeId: String,
        val bikeModel: String?,
        val status: String,
        // ISO-8601 string — see the note in GetLeasingApplicationController.LeasingApplicationDto.
        @field:JsonFormat(shape = JsonFormat.Shape.STRING)
        val createdAt: LocalDateTime,
    )
}
