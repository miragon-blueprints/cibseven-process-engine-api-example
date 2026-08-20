package io.miragon.blueprint.adapter.inbound.rest

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.miragon.blueprint.application.port.inbound.SubmitLeasingRequestUseCase
import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.leasing.CustomerName
import io.miragon.blueprint.domain.leasing.Email
import io.swagger.v3.oas.annotations.Operation
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/bike-leasing")
class SubmitLeasingRequestController(
    private val useCase: SubmitLeasingRequestUseCase,
) {

    private val log = KotlinLogging.logger {}

    @Operation(operationId = "submitLeasingRequest")
    @PostMapping
    fun submit(@RequestBody input: LeasingRequestInput): ResponseEntity<LeasingApplicationCreatedDto> {
        log.debug { "Received leasing request: $input" }
        val id = useCase.submit(input.toCommand())
        return ResponseEntity.ok(LeasingApplicationCreatedDto(id.value.toString()))
    }

    data class LeasingRequestInput
        @JsonCreator
        constructor(
            @JsonProperty("customerName") val customerName: String,
            @JsonProperty("email") val email: String,
            @JsonProperty("age") val age: Int,
            @JsonProperty("monthlyNetIncome") val monthlyNetIncome: Double,
            @JsonProperty("bikeId") val bikeId: String,
            @JsonProperty("bikeModel") val bikeModel: String,
        )

    data class LeasingApplicationCreatedDto(val applicationId: String)

    private fun LeasingRequestInput.toCommand() =
        SubmitLeasingRequestUseCase.Command(
            customerName = CustomerName(customerName),
            email = Email(email),
            age = age,
            monthlyNetIncome = monthlyNetIncome,
            bikeId = BikeId(bikeId),
            bikeModel = bikeModel,
        )
}
