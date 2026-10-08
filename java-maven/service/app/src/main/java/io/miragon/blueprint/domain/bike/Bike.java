package io.miragon.blueprint.domain.bike;

/**
 * A bike in MiraVelo's portfolio. The leasing process only ever carries the {@code bikeId}; the
 * descriptive {@code model} lives here, in a separate aggregate persisted on its own, so it never travels
 * through the engine as a process variable.
 */
public record Bike(
        BikeId bikeId,
        String model) {
}
