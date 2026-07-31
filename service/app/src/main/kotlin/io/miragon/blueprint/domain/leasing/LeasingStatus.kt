package io.miragon.blueprint.domain.leasing

/**
 * Lifecycle of a leasing application, mirrored from the process:
 * [RECEIVED] on submission, [ORDERED] once a bike order exists, [ACTIVE] when the leasing is live,
 * and the two terminal negative outcomes [REJECTED] and [CANCELLED].
 */
enum class LeasingStatus {
    RECEIVED,
    ORDERED,
    ACTIVE,
    REJECTED,
    CANCELLED,
}
