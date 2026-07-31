package io.miragon.blueprint.adapter.inbound.rest

import com.ninjasquad.springmockk.MockkBean
import io.miragon.blueprint.application.port.inbound.GetLeasingApplicationQuery
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.testLeasingApplication
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

@WebMvcTest(GetLeasingApplicationController::class)
class GetLeasingApplicationControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var query: GetLeasingApplicationQuery

    @Test
    fun `returns the application with its resolved bike model when it exists`() {

        // given: an application the query can find, with its bike model resolved from the portfolio
        val application = testLeasingApplication()
        every { query.byId(application.id) } returns GetLeasingApplicationQuery.Result(application, "Gravel Explorer 900")
        val operation = get("/api/bike-leasing/{applicationId}", application.id.value.toString())

        // when: the request is performed
        val response = mockMvc.perform(operation).andReturn()

        // then: the response is 200 with the application's id, status and resolved bike model
        assertThat(response.response.status).isEqualTo(200)
        assertThat(response.response.contentAsString)
            .contains(application.id.value.toString(), "RECEIVED", "Gravel Explorer 900")
        verify { query.byId(application.id) }
        confirmVerified(query)
    }

    @Test
    fun `returns 404 when the application does not exist`() {

        // given: an unknown application id
        val id = ApplicationId.of("123e4567-e89b-12d3-a456-426614174000")
        every { query.byId(id) } returns null
        val operation = get("/api/bike-leasing/{applicationId}", id.value.toString())

        // when: the request is performed
        val response = mockMvc.perform(operation).andReturn()

        // then: the response is 404 Not Found
        assertThat(response.response.status).isEqualTo(404)
    }
}
