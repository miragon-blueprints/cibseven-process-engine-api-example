package io.miragon.blueprint.application.port.inbound;

import io.miragon.blueprint.domain.leasing.PendingClarification;
import java.util.List;

/** Lists the applications waiting on the alternative-clarification user task — the back-office inbox. */
public interface GetPendingClarificationsQuery {

    List<PendingClarification> pending();
}
