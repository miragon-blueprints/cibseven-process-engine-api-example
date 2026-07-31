package io.miragon.blueprint.adapter.inbound.rest

import io.miragon.blueprint.application.port.inbound.GetLeasingApplicationQuery
import io.miragon.blueprint.domain.leasing.ApplicationId
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/bike-leasing")
class GetLeasingApplicationController(
    private val query: GetLeasingApplicationQuery,
) {

    @GetMapping("/{applicationId}")
    fun byId(@PathVariable applicationId: String): ResponseEntity<LeasingApplicationDto> {
        val result = query.byId(ApplicationId.of(applicationId)) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(result.toDto())
    }

    data class LeasingApplicationDto(
        val applicationId: String,
        val customerName: String,
        val email: String,
        val bikeId: String,
        val bikeModel: String?,
        val status: String,
        val orderId: String?,
        val contractId: String?,
    )

    private fun GetLeasingApplicationQuery.Result.toDto() =
        LeasingApplicationDto(
            applicationId = application.id.value.toString(),
            customerName = application.customerName.value,
            email = application.email.value,
            bikeId = application.bikeId.value,
            // resolved from the bike portfolio, not carried on the application
            bikeModel = bikeModel,
            status = application.status.name,
            orderId = application.orderId?.value,
            contractId = application.contractId?.value,
        )
}
