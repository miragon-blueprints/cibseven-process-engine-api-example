package io.miragon.blueprint.domain.leasing

/** Reference to the leasing contract issued by the (external) contract system. */
@JvmInline
value class ContractId(val value: String) {
    init {
        require(value.isNotBlank()) { "ContractId must not be blank" }
    }
}
