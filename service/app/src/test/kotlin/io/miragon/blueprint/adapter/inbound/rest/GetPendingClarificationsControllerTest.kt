package io.miragon.blueprint.adapter.inbound.rest

import com.ninjasquad.springmockk.MockkBean
import io.miragon.blueprint.application.port.inbound.GetPendingClarificationsQuery
import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.CustomerName
import io.miragon.blueprint.domain.leasing.PendingClarification
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(GetPendingClarificationsController::class)
class GetPendingClarificationsControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var query: GetPendingClarificationsQuery

    private val id = ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))

    @Test
    fun `returns the pending clarifications without exposing a task id`() {
        // given: one application waiting on the clarify-alternative task
        every { query.pending() } returns
            listOf(
                PendingClarification(
                    applicationId = id,
                    customerName = CustomerName("Erin Restock"),
                    requestedBikeId = BikeId("BIKE-OOS"),
                    requestedBikeModel = "Mountain Trail 600",
                    waitingSince = LocalDateTime.of(2026, 8, 17, 9, 30),
                ),
            )

        // when
        val response = mockMvc.perform(get("/api/tasks/clarify-alternative")).andReturn()

        // then: the case fields are present, and no `taskId` field leaks into the payload
        assertThat(response.response.status).isEqualTo(200)
        val body = response.response.contentAsString
        assertThat(body).contains(id.value.toString(), "Erin Restock", "BIKE-OOS", "Mountain Trail 600")
        assertThat(body).doesNotContain("taskId")
    }

    @Test
    fun `returns an empty array when the inbox is empty`() {
        // given
        every { query.pending() } returns emptyList()

        // when / then
        val response = mockMvc.perform(get("/api/tasks/clarify-alternative")).andReturn()
        assertThat(response.response.status).isEqualTo(200)
        assertThat(response.response.contentAsString).isEqualTo("[]")
    }
}
