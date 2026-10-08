package io.miragon.blueprint.application.port.inbound;

import io.miragon.blueprint.domain.leasing.ApplicationId;

/** Reminds the customer that the contract is still awaiting signature. */
@FunctionalInterface
public interface SendSignatureReminderUseCase {

    void sendSignatureReminder(ApplicationId id);
}
