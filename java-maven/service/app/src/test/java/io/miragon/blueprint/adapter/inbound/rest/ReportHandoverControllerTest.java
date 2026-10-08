package io.miragon.blueprint.adapter.inbound.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import io.miragon.blueprint.application.port.inbound.ReportHandoverUseCase;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;

@WebMvcTest(ReportHandoverController.class)
class ReportHandoverControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportHandoverUseCase useCase;

    @Test
    void dealer_reports_the_handover() throws Exception {

        // given: a valid application-id path variable & rest-operation
        String pathVar = "123e4567-e89b-12d3-a456-426614174000";
        doNothing().when(useCase).reportHandover(any());
        RequestBuilder operation = post("/api/bike-leasing/{applicationId}/report-handover", pathVar);

        // when: the request is performed
        MvcResult response = mockMvc.perform(operation).andReturn();

        // then: the use case is invoked and the response is 202 Accepted
        assertThat(response.getResponse().getStatus()).isEqualTo(202);
        verify(useCase).reportHandover(ApplicationId.of(pathVar));
        verifyNoMoreInteractions(useCase);
    }
}
