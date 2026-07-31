package io.miragon.blueprint.adapter.inbound.rest

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.miragon.blueprint.application.port.inbound.SelectAlternativeUseCase
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.bike.BikeId
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * External completion of the `Clarify alternative with customer` user task — the counterpart to a
 * human completing the deployed Camunda Form in the CIB seven Tasklist.
 */
@RestController
@RequestMapping("/api/bike-leasing")
class SelectAlternativeController(
    private val useCase: SelectAlternativeUseCase,
) {

    @PostMapping("/{applicationId}/clarify-alternative")
    fun clarifyAlternative(
        @PathVariable applicationId: String,
        @RequestBody input: AlternativeDecisionInput,
    ): ResponseEntity<Unit> {
        useCase.selectAlternative(
            SelectAlternativeUseCase.Command(
                applicationId = ApplicationId.of(applicationId),
                alternativeFound = input.alternativeFound,
                bikeId = input.bikeId?.let { BikeId(it) },
                bikeModel = input.bikeModel,
            ),
        )
        return ResponseEntity.accepted().build()
    }

    data class AlternativeDecisionInput
        @JsonCreator
        constructor(
            @JsonProperty("alternativeFound") val alternativeFound: Boolean,
            @JsonProperty("bikeId") val bikeId: String? = null,
            @JsonProperty("bikeModel") val bikeModel: String? = null,
        )
}
