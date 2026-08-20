package io.miragon.blueprint.application.port.outbound

import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.LeasingApplication
import io.miragon.blueprint.domain.leasing.LeasingStatus

interface LeasingApplicationRepository {
    fun save(application: LeasingApplication): LeasingApplication

    fun findById(id: ApplicationId): LeasingApplication?

    /**
     * Reads a page of applications, optionally filtered by status. The [Criteria] and [Page] are the
     * port's own types on purpose — Spring Data's `Pageable`/`Page` stop at the persistence adapter so
     * the application layer never depends on a persistence technology.
     */
    fun findAll(criteria: Criteria): Page

    data class Criteria(
        val status: LeasingStatus?,
        val page: Int,
        val size: Int,
    )

    data class Page(
        val items: List<LeasingApplication>,
        val page: Int,
        val size: Int,
        val totalElements: Long,
        val totalPages: Int,
    )
}
