package io.miragon.blueprint.domain.bike;

/**
 * Raised when the dealer cannot deliver the requested bike, so no order was placed. The inbound
 * CIB seven adapter translates this into the BPMN error {@code bikeUnavailable}, which the process
 * catches on the order task's boundary event.
 */
public final class BikeUnavailableException extends RuntimeException {

    public BikeUnavailableException(BikeId bikeId) {
        super("Bike " + bikeId.value() + " is not available at the dealer");
    }
}
