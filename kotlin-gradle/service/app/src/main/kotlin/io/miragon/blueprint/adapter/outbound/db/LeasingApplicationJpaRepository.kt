package io.miragon.blueprint.adapter.outbound.db

import io.miragon.blueprint.domain.leasing.LeasingStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface LeasingApplicationJpaRepository : JpaRepository<LeasingApplicationEntity, UUID> {
    fun findByApplicationId(id: UUID): LeasingApplicationEntity?

    fun findAllByStatus(status: LeasingStatus, pageable: Pageable): Page<LeasingApplicationEntity>
}
