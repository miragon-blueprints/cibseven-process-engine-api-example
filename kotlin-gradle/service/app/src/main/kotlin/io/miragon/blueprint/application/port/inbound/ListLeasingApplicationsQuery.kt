package io.miragon.blueprint.application.port.inbound

import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.CustomerName
import io.miragon.blueprint.domain.leasing.LeasingStatus
import java.time.LocalDateTime

/**
 * Reads a page of leasing applications for the customer-portal list, optionally filtered by status.
 * The port owns its [Filter]/[Item]/[Page] types; Spring Data paging never crosses into the
 * application layer.
 */
interface ListLeasingApplicationsQuery {
    fun list(filter: Filter): Page

    data class Filter(
        val status: LeasingStatus?,
        val page: Int,
        val size: Int,
    )

    data class Item(
        val applicationId: ApplicationId,
        val customerName: CustomerName,
        val bikeId: BikeId,
        val bikeModel: String?,
        val status: LeasingStatus,
        val createdAt: LocalDateTime,
    )

    data class Page(
        val items: List<Item>,
        val page: Int,
        val size: Int,
        val totalElements: Long,
        val totalPages: Int,
    )
}
