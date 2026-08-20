package io.miragon.blueprint.domain.leasing

/**
 * Lifecycle of a leasing application, mirrored from the process:
 * [RECEIVED] on submission, [ORDERED] once a bike order exists, [HANDED_OVER] once the bike is
 * handed to the customer (waiting-period token), [ACTIVE] when the leasing is live after the
 * withdrawal period elapses. [WITHDRAWN] is the in-progress cancellation state: the customer has
 * withdrawn and the compensation runs asynchronously (it may park on a return-clarification task)
 * before it reaches the terminal [CANCELLED]. [REJECTED] and [CANCELLED] are the terminal negative
 * outcomes.
 */
enum class LeasingStatus {
    RECEIVED,
    ORDERED,
    HANDED_OVER,
    ACTIVE,
    WITHDRAWN,
    REJECTED,
    CANCELLED,
}
