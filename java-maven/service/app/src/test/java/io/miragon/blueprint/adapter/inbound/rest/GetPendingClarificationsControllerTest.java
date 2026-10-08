package io.miragon.blueprint.adapter.inbound.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import io.miragon.blueprint.application.port.inbound.GetPendingClarificationsQuery;
import io.miragon.blueprint.domain.bike.BikeId;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import io.miragon.blueprint.domain.leasing.CustomerName;
import io.miragon.blueprint.domain.leasing.PendingClarification;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(GetPendingClarificationsController.class)
class GetPendingClarificationsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetPendingClarificationsQuery query;

    private final ApplicationId id = new ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));

    @Test
    void returns_the_pending_clarifications_without_exposing_a_task_id() throws Exception {
        // given: one application waiting on the clarify-alternative task
        when(query.pending()).thenReturn(
                List.of(
                        new PendingClarification(
                                id,
                                new CustomerName("Erin Restock"),
                                new BikeId("BIKE-OOS"),
                                "Mountain Trail 600",
                                LocalDateTime.of(2026, 8, 17, 9, 30))));

        // when
        MvcResult response = mockMvc.perform(get("/api/tasks/clarify-alternative")).andReturn();

        // then: the case fields are present, and no `taskId` field leaks into the payload
        assertThat(response.getResponse().getStatus()).isEqualTo(200);
        String body = response.getResponse().getContentAsString();
        assertThat(body).contains(id.value().toString(), "Erin Restock", "BIKE-OOS", "Mountain Trail 600");
        assertThat(body).doesNotContain("taskId");
    }

    @Test
    void returns_an_empty_array_when_the_inbox_is_empty() throws Exception {
        // given
        when(query.pending()).thenReturn(List.of());

        // when / then
        MvcResult response = mockMvc.perform(get("/api/tasks/clarify-alternative")).andReturn();
        assertThat(response.getResponse().getStatus()).isEqualTo(200);
        assertThat(response.getResponse().getContentAsString()).isEqualTo("[]");
    }
}
