package io.miragon.blueprint.adapter.inbound.rest

import com.ninjasquad.springmockk.MockkBean
import io.miragon.blueprint.application.port.inbound.ListBikesQuery
import io.miragon.blueprint.domain.bike.BikeId
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

@WebMvcTest(ListBikesController::class)
class ListBikesControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var query: ListBikesQuery

    @Test
    fun `returns the catalogue with availability flags`() {
        // given: a catalogue with one available and one out-of-stock bike
        every { query.all() } returns
            listOf(
                ListBikesQuery.Item(BikeId("BIKE-900"), "Gravel Explorer 900", available = true),
                ListBikesQuery.Item(BikeId("BIKE-OOS"), "Mountain Trail 600", available = false),
            )

        // when
        val response = mockMvc.perform(get("/api/bikes")).andReturn()

        // then: both bikes and their availability are serialised
        assertThat(response.response.status).isEqualTo(200)
        assertThat(response.response.contentAsString).contains(
            "BIKE-900",
            "Gravel Explorer 900",
            "BIKE-OOS",
            "\"available\":true",
            "\"available\":false",
        )
    }
}
