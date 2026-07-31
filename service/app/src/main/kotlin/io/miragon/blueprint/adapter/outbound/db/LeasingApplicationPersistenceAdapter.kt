package io.miragon.blueprint.adapter.outbound.db

import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.LeasingApplication
import org.springframework.stereotype.Component

@Component
class LeasingApplicationPersistenceAdapter(
    private val repository: LeasingApplicationJpaRepository,
) : LeasingApplicationRepository {

    override fun save(application: LeasingApplication): LeasingApplication {
        val entity = repository.save(LeasingApplicationEntityMapper.toEntity(application))
        return LeasingApplicationEntityMapper.toDomain(entity)
    }

    override fun findById(id: ApplicationId): LeasingApplication? =
        repository.findByApplicationId(id.value)?.let { LeasingApplicationEntityMapper.toDomain(it) }
}
