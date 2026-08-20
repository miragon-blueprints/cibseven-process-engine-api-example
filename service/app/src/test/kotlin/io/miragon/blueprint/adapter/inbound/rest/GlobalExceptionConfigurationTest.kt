package io.miragon.blueprint.adapter.inbound.rest

import com.ninjasquad.springmockk.MockkBean
import io.miragon.blueprint.application.port.inbound.ReportHandoverUseCase
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.cibseven.bpm.engine.MismatchingMessageCorrelationException
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

@WebMvcTest(ReportHandoverController::class)
class GlobalExceptionConfigurationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var useCase: ReportHandoverUseCase

    @Test
    fun `MismatchingMessageCorrelationException maps to 409 with problem+json`() {

        // given: a correlation that fails because the token is no longer at the expected wait state
        val pathVar = "123e4567-e89b-12d3-a456-426614174000"
        every { useCase.reportHandover(any()) } throws
            MismatchingMessageCorrelationException("miravelo.handoverReported", "No matching token")

        // when: the POST is performed
        val response =
            mockMvc
                .perform(post("/api/bike-leasing/{applicationId}/report-handover", pathVar))
                .andReturn()

        // then: 409 Conflict is returned as application/problem+json
        assertThat(response.response.status).isEqualTo(409)
        assertThat(response.response.contentType).contains(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
    }
}
