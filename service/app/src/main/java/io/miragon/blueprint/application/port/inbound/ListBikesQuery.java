package io.miragon.blueprint.application.port.inbound;

import io.miragon.blueprint.domain.bike.BikeId;
import java.util.List;

/**
 * Lists MiraVelo's bike catalogue with current availability, so the submit form offers a picker
 * instead of a free-text bike id. Unavailable bikes stay selectable on purpose — that is how a user
 * drives the bike-unavailable scenario from the UI.
 */
public interface ListBikesQuery {

    List<Item> all();

    record Item(
            BikeId bikeId,
            String model,
            boolean available) {
    }
}
