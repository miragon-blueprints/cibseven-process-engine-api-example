package io.miragon.blueprint.adapter.inbound.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.miragon.blueprint.application.port.inbound.SelectAlternativeUseCase
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.bike.BikeId
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

@WebMvcTest(SelectAlternativeController::class)
class SelectAlternativeControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var useCase: SelectAlternativeUseCase

    private val mapper = ObjectMapper()

    @Test
    fun `client clarifies an alternative bike`() {

        // given: a decision body and the application-id path variable
        val pathVar = "123e4567-e89b-12d3-a456-426614174000"
        val body = mapOf("alternativeFound" to true, "bikeId" to "BIKE-ALT", "bikeModel" to "Aero Road 700")
        every { useCase.selectAlternative(any()) } just Runs
        val operation =
            post("/api/bike-leasing/{applicationId}/clarify-alternative", pathVar)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(body))

        // when: the request is performed
        val response = mockMvc.perform(operation).andReturn()

        // then: the use case is invoked with the mapped command and the response is 202 Accepted
        assertThat(response.response.status).isEqualTo(202)
        verify {
            useCase.selectAlternative(
                SelectAlternativeUseCase.Command(ApplicationId.of(pathVar), alternativeFound = true, bikeId = BikeId("BIKE-ALT"), bikeModel = "Aero Road 700"),
            )
        }
        confirmVerified(useCase)
    }

    @Test
    fun `client reports that no alternative was found`() {

        // given: a decision body with no alternative and no bike details
        val pathVar = "123e4567-e89b-12d3-a456-426614174000"
        val body = mapOf("alternativeFound" to false)
        every { useCase.selectAlternative(any()) } just Runs
        val operation =
            post("/api/bike-leasing/{applicationId}/clarify-alternative", pathVar)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(body))

        // when: the request is performed
        val response = mockMvc.perform(operation).andReturn()

        // then: the use case is invoked with a command carrying no alternative bike and the response is 202
        assertThat(response.response.status).isEqualTo(202)
        verify {
            useCase.selectAlternative(
                SelectAlternativeUseCase.Command(ApplicationId.of(pathVar), alternativeFound = false, bikeId = null, bikeModel = null),
            )
        }
        confirmVerified(useCase)
    }
}
