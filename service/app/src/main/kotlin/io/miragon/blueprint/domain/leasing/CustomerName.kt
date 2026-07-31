package io.miragon.blueprint.domain.leasing

@JvmInline
value class CustomerName(val value: String) {
    init {
        require(value.isNotBlank()) { "Customer name must not be blank" }
    }
}
