package io.miragon.blueprint.adapter.inbound.rest

import com.ninjasquad.springmockk.MockkBean
import io.miragon.blueprint.application.port.inbound.GetLeasingApplicationQuery
import io.miragon.blueprint.domain.bike.OrderId
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.ContractId
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
    fun `returns the application with all of its fields mapped when it exists`() {

        // given: a fully-populated application (incl. order and contract) the query can find
        val application = testLeasingApplication(
            orderId = OrderId("ORDER-42"),
            contractId = ContractId("CONTRACT-7"),
        )
        every { query.byId(application.id) } returns GetLeasingApplicationQuery.Result(application, "Gravel Explorer 900")
        val operation = get("/api/bike-leasing/{applicationId}", application.id.value.toString())

        // when: the request is performed
        val response = mockMvc.perform(operation).andReturn()

        // then: the response is 200 and every DTO field is mapped from the domain object
        assertThat(response.response.status).isEqualTo(200)
        assertThat(response.response.contentAsString).contains(
            application.id.value.toString(),
            application.customerName.value,
            application.email.value,
            application.bikeId.value,
            "Gravel Explorer 900",
            "RECEIVED",
            "ORDER-42",
            "CONTRACT-7",
        )
        verify { query.byId(application.id) }
        confirmVerified(query)
    }

    @Test
    fun `maps a not-yet-ordered application with null order and contract`() {

        // given: an application still without an order or contract (both nullable fields absent)
        val application = testLeasingApplication(orderId = null, contractId = null)
        every { query.byId(application.id) } returns GetLeasingApplicationQuery.Result(application, null)
        val operation = get("/api/bike-leasing/{applicationId}", application.id.value.toString())

        // when: the request is performed
        val response = mockMvc.perform(operation).andReturn()

        // then: the response is 200 and the nullable fields are serialised as null
        assertThat(response.response.status).isEqualTo(200)
        assertThat(response.response.contentAsString)
            .contains("\"orderId\":null", "\"contractId\":null", "\"bikeModel\":null")
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
