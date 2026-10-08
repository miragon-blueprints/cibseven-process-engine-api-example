package io.miragon.blueprint.application.port.outbound

import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.leasing.LeasingApplication

/**
 * Outbound port that drives the BPMN engine: it starts a process instance, correlates the messages
 * that release the process's wait states, and completes the alternative-clarification user task from
 * the outside. Implemented by the CIB seven adapter.
 */
interface LeasingProcess {
    fun submitRequest(application: LeasingApplication)

    fun correlateContractSigned(id: ApplicationId)

    fun correlateHandoverReported(id: ApplicationId)

    fun correlateApplicationWithdrawn(id: ApplicationId)

    fun completeAlternativeClarification(
        id: ApplicationId,
        alternativeFound: Boolean,
        bikeId: BikeId? = null,
    )
}
