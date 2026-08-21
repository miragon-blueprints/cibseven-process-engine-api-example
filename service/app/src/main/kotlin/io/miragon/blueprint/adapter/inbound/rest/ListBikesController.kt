package io.miragon.blueprint.adapter.inbound.rest

import io.miragon.blueprint.application.port.inbound.ListBikesQuery
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * `GET /api/bikes` — the bike catalogue for the submit form's picker. Exists so nobody has to type a
 * bike id by hand; unavailable bikes (e.g. `BIKE-OOS`) stay in the list on purpose, so a user can
 * drive the bike-unavailable scenario from the UI.
 */
@RestController
@RequestMapping("/api/bikes")
class ListBikesController(
    private val query: ListBikesQuery,
) {

    @Operation(operationId = "listBikes")
    @GetMapping
    fun all(): List<BikeDto> =
        query.all().map { BikeDto(bikeId = it.bikeId.value, model = it.model, available = it.available) }

    data class BikeDto(
        val bikeId: String,
        val model: String,
        val available: Boolean,
    )
}
