package io.miragon.blueprint.domain.bike

/** Identifies the concrete bike a leasing application is about — carried through to the order. */
@JvmInline
value class BikeId(val value: String) {
    init {
        require(value.isNotBlank()) { "BikeId must not be blank" }
    }
}
