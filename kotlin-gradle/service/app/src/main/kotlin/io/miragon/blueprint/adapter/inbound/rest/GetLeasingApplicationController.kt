package io.miragon.blueprint.adapter.inbound.rest

import com.fasterxml.jackson.annotation.JsonFormat
import io.miragon.blueprint.application.port.inbound.GetLeasingApplicationQuery
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import java.time.LocalDateTime
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/bike-leasing")
class GetLeasingApplicationController(
    private val query: GetLeasingApplicationQuery,
) {

    @Operation(operationId = "getLeasingApplication")
    @GetMapping("/{applicationId}")
    fun byId(@PathVariable applicationId: String): ResponseEntity<LeasingApplicationDto> {
        val result = query.byId(ApplicationId.of(applicationId)) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(result.toDto())
    }

    data class LeasingApplicationDto(
        val applicationId: String,
        val customerName: String,
        val email: String,
        val age: Int,
        val monthlyNetIncome: Double,
        val bikeId: String,
        val bikeModel: String?,
        val status: String,
        val orderId: String?,
        val contractId: String?,
        // Force ISO-8601 string form: Jackson 3 (SB4) defaults to a numeric array, but the CIB seven
        // webapp serves /api with its own Jackson mapper that ignores our global date-time config, so
        // the format is pinned at the field to keep the payload in sync with the springdoc contract
        // any API consumer relies on.
        @field:JsonFormat(shape = JsonFormat.Shape.STRING)
        val createdAt: LocalDateTime,
    )

    private fun GetLeasingApplicationQuery.Result.toDto() =
        LeasingApplicationDto(
            applicationId = application.id.value.toString(),
            customerName = application.customerName.value,
            email = application.email.value,
            age = application.age,
            monthlyNetIncome = application.monthlyNetIncome,
            bikeId = application.bikeId.value,
            // resolved from the bike portfolio, not carried on the application
            bikeModel = bikeModel,
            status = application.status.name,
            orderId = application.orderId?.value,
            contractId = application.contractId?.value,
            createdAt = application.createdAt,
        )
}
