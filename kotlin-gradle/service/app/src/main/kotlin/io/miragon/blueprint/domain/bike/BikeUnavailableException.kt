package io.miragon.blueprint.domain.bike

/**
 * Raised when the dealer cannot deliver the requested bike, so no order was placed. The inbound
 * CIB seven adapter translates this into the BPMN error `bikeUnavailable`, which the process catches
 * on the order task's boundary event.
 */
class BikeUnavailableException(
    bikeId: BikeId,
) : RuntimeException("Bike ${bikeId.value} is not available at the dealer")
