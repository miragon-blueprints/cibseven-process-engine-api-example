package io.miragon.blueprint.application.service

import io.miragon.blueprint.application.port.inbound.GetPendingClarificationsQuery
import io.miragon.blueprint.application.port.outbound.BikePortfolioRepository
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.application.port.outbound.TaskInboxPort
import io.miragon.blueprint.domain.leasing.PendingClarification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GetPendingClarificationsService(
    private val taskInbox: TaskInboxPort,
    private val repository: LeasingApplicationRepository,
    private val bikePortfolio: BikePortfolioRepository,
) : GetPendingClarificationsQuery {

    override fun pending(): List<PendingClarification> {
        val openTasks = taskInbox.findOpenClarifications()
        // Pair each open task with its application; skip tasks whose application vanished (defensive).
        val cases = openTasks.mapNotNull { task ->
            repository.findById(task.applicationId)?.let { application -> task to application }
        }
        val models = bikePortfolio
            .findAllByIds(cases.map { (_, application) -> application.bikeId })
            .associate { it.bikeId to it.model }
        return cases.map { (task, application) ->
            PendingClarification(
                applicationId = application.id,
                customerName = application.customerName,
                requestedBikeId = application.bikeId,
                requestedBikeModel = models[application.bikeId],
                waitingSince = task.waitingSince,
            )
        }
    }
}
