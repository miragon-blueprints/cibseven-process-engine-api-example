package io.miragon.blueprint.application.port.outbound;

import io.miragon.blueprint.domain.leasing.ApplicationId;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Reads open user tasks from the process engine's task list. Kept deliberately thin: it returns only
 * the business key (the application id) and when the task appeared, never an engine task id. The
 * back-office resolves a clarification through the domain ({@code POST .../clarify-alternative}), which
 * correlates by business key — so the UI never needs, and can never misuse, a raw task id.
 */
public interface TaskInboxPort {

    List<OpenClarification> findOpenClarifications();

    record OpenClarification(
            ApplicationId applicationId,
            LocalDateTime waitingSince) {
    }
}
