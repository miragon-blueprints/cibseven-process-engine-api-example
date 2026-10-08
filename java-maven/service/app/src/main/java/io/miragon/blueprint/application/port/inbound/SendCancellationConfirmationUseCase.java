package io.miragon.blueprint.application.port.inbound;

import io.miragon.blueprint.domain.leasing.ApplicationId;

/** Confirms to the customer that the application was cancelled. */
@FunctionalInterface
public interface SendCancellationConfirmationUseCase {

    void sendCancellationConfirmation(ApplicationId id);
}
