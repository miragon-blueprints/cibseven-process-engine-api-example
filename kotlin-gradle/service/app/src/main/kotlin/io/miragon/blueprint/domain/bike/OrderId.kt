package io.miragon.blueprint.domain.bike

@JvmInline
value class OrderId(val value: String) {
    init {
        require(value.isNotBlank()) { "OrderId must not be blank" }
    }
}
