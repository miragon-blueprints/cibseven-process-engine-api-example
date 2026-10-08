package io.miragon.blueprint.adapter.inbound.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.miragon.blueprint.application.port.inbound.SubmitLeasingRequestUseCase
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.bike.BikeId
import io.miragon.blueprint.domain.leasing.CustomerName
import io.miragon.blueprint.domain.leasing.Email
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import java.util.UUID

@WebMvcTest(SubmitLeasingRequestController::class)
class SubmitLeasingRequestControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var useCase: SubmitLeasingRequestUseCase

    private val mapper = ObjectMapper()

    @Test
    fun `user submits a leasing request`() {

        // given: valid input data & rest-operation
        val input =
            mapOf(
                "customerName" to "John Doe",
                "email" to "john.doe@test.com",
                "age" to 35,
                "monthlyNetIncome" to 3500.0,
                "bikeId" to "BIKE-900",
                "bikeModel" to "Gravel Explorer 900",
            )
        val applicationId = ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))
        val expectedCommand =
            SubmitLeasingRequestUseCase.Command(
                customerName = CustomerName("John Doe"),
                email = Email("john.doe@test.com"),
                age = 35,
                monthlyNetIncome = 3500.0,
                bikeId = BikeId("BIKE-900"),
                bikeModel = "Gravel Explorer 900",
            )
        every { useCase.submit(any()) } returns applicationId
        val operation =
            post("/api/bike-leasing")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(input))

        // when: the request is performed
        val response = mockMvc.perform(operation).andReturn()

        // then: the use case is invoked with the mapped command and the id is returned
        assertThat(response.response.status).isEqualTo(200)
        assertThat(response.response.contentAsString).contains(applicationId.value.toString())
        verify { useCase.submit(expectedCommand) }
        confirmVerified(useCase)
    }
}
