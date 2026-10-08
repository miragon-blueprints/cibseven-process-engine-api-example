package io.miragon.blueprint.adapter.inbound.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.miragon.blueprint.application.port.inbound.ReportHandoverUseCase;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import org.cibseven.bpm.engine.MismatchingMessageCorrelationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(ReportHandoverController.class)
class GlobalExceptionConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportHandoverUseCase useCase;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void MismatchingMessageCorrelationException_maps_to_409_with_problem_json() throws Exception {

        // given: a correlation that fails because the token is no longer at the expected wait state
        String pathVar = "123e4567-e89b-12d3-a456-426614174000";
        MismatchingMessageCorrelationException exception =
                new MismatchingMessageCorrelationException("miravelo.handoverReported", "No matching token");
        doThrow(exception).when(useCase).reportHandover(any());

        // when: the POST is performed
        MvcResult response =
                mockMvc
                        .perform(post("/api/bike-leasing/{applicationId}/report-handover", pathVar))
                        .andReturn();

        // then: 409 Conflict is returned as application/problem+json
        assertThat(response.getResponse().getStatus()).isEqualTo(409);
        assertThat(response.getResponse().getContentType()).contains(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        // and: the RFC 9457 fields carry the advice's title, type and the exception message
        JsonNode problem = mapper.readTree(response.getResponse().getContentAsString());
        assertThat(problem.path("status").asInt()).isEqualTo(409);
        assertThat(problem.path("title").asText()).isEqualTo("Action not available in the current state");
        assertThat(problem.path("type").asText()).isEqualTo("https://miravelo.example/problems/409");
        assertThat(problem.path("detail").asText()).isEqualTo(exception.getMessage());
    }

    @Test
    void IllegalStateException_for_an_unknown_application_maps_to_404_with_problem_json() throws Exception {

        // given: the use case signals a missing application with an IllegalStateException
        String pathVar = "123e4567-e89b-12d3-a456-426614174000";
        ApplicationId id = ApplicationId.of(pathVar);
        doThrow(new IllegalStateException("Unknown application " + id)).when(useCase).reportHandover(any());

        // when: the POST is performed
        MvcResult response =
                mockMvc
                        .perform(post("/api/bike-leasing/{applicationId}/report-handover", pathVar))
                        .andReturn();

        // then: 404 Not Found is returned as application/problem+json carrying the service's message
        assertThat(response.getResponse().getStatus()).isEqualTo(404);
        assertThat(response.getResponse().getContentType()).contains(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        JsonNode problem = mapper.readTree(response.getResponse().getContentAsString());
        assertThat(problem.path("status").asInt()).isEqualTo(404);
        assertThat(problem.path("title").asText()).isEqualTo("Resource not found");
        assertThat(problem.path("type").asText()).isEqualTo("https://miravelo.example/problems/404");
        assertThat(problem.path("detail").asText()).isEqualTo("Unknown application " + id);
    }
}
