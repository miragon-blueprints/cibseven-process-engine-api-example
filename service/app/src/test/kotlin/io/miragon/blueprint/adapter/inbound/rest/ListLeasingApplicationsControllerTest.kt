package io.miragon.blueprint.adapter.inbound.rest

import com.ninjasquad.springmockk.MockkBean
import io.miragon.blueprint.application.port.inbound.ListLeasingApplicationsQuery
import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.CustomerName
import io.miragon.blueprint.domain.leasing.LeasingStatus
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(ListLeasingApplicationsController::class)
class ListLeasingApplicationsControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var query: ListLeasingApplicationsQuery

    private val id = ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))

    @Test
    fun `returns a page of application summaries`() {
        // given: a single-item page
        every { query.list(any()) } returns
            ListLeasingApplicationsQuery.Page(
                items = listOf(
                    ListLeasingApplicationsQuery.Item(
                        applicationId = id,
                        customerName = CustomerName("Alice Rider"),
                        bikeId = BikeId("BIKE-900"),
                        bikeModel = "Gravel Explorer 900",
                        status = LeasingStatus.RECEIVED,
                        createdAt = LocalDateTime.of(2026, 8, 17, 10, 30),
                    ),
                ),
                page = 0,
                size = 20,
                totalElements = 1,
                totalPages = 1,
            )

        // when
        val response = mockMvc.perform(get("/api/bike-leasing")).andReturn()

        // then
        assertThat(response.response.status).isEqualTo(200)
        assertThat(response.response.contentAsString).contains(
            id.value.toString(),
            "Alice Rider",
            "BIKE-900",
            "Gravel Explorer 900",
            "RECEIVED",
            "\"totalElements\":1",
        )
    }

    @Test
    fun `parses the status filter and paging params into the query`() {
        // given
        every { query.list(any()) } returns
            ListLeasingApplicationsQuery.Page(emptyList(), page = 2, size = 5, totalElements = 0, totalPages = 0)
        val filter = slot<ListLeasingApplicationsQuery.Filter>()

        // when: a filtered request is made with lowercase status
        mockMvc.perform(get("/api/bike-leasing?status=active&page=2&size=5")).andReturn()

        // then: the parsed filter reaches the query
        verify { query.list(capture(filter)) }
        assertThat(filter.captured.status).isEqualTo(LeasingStatus.ACTIVE)
        assertThat(filter.captured.page).isEqualTo(2)
        assertThat(filter.captured.size).isEqualTo(5)
    }

    @Test
    fun `rejects an unknown status with a 400 problem detail`() {
        // when: an invalid status is requested
        val response = mockMvc.perform(get("/api/bike-leasing?status=bogus")).andReturn()

        // then: the global advice turns it into an RFC 9457 problem response
        assertThat(response.response.status).isEqualTo(400)
        assertThat(response.response.contentType).contains("application/problem+json")
        assertThat(response.response.contentAsString).contains("unknown status 'bogus'")
    }
}
