package io.miragon.blueprint.application.port.inbound

import io.miragon.blueprint.domain.bike.BikeId

/**
 * Lists MiraVelo's bike catalogue with current availability, so the submit form offers a picker
 * instead of a free-text bike id. Unavailable bikes stay selectable on purpose — that is how a user
 * drives the bike-unavailable scenario from the UI.
 */
interface ListBikesQuery {
    fun all(): List<Item>

    data class Item(
        val bikeId: BikeId,
        val model: String,
        val available: Boolean,
    )
}
