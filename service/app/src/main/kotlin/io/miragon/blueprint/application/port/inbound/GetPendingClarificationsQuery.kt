package io.miragon.blueprint.application.port.inbound

import io.miragon.blueprint.domain.leasing.PendingClarification

/** Lists the applications waiting on the alternative-clarification user task — the back-office inbox. */
interface GetPendingClarificationsQuery {
    fun pending(): List<PendingClarification>
}
