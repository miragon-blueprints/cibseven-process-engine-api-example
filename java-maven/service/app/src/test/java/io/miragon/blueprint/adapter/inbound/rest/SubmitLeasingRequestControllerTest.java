package io.miragon.blueprint.adapter.inbound.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.miragon.blueprint.application.port.inbound.SubmitLeasingRequestUseCase;
import io.miragon.blueprint.domain.bike.BikeId;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import io.miragon.blueprint.domain.leasing.CustomerName;
import io.miragon.blueprint.domain.leasing.Email;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;

@WebMvcTest(SubmitLeasingRequestController.class)
class SubmitLeasingRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubmitLeasingRequestUseCase useCase;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void user_submits_a_leasing_request() throws Exception {

        // given: valid input data & rest-operation
        Map<String, Object> input =
                Map.of(
                        "customerName", "John Doe",
                        "email", "john.doe@test.com",
                        "age", 35,
                        "monthlyNetIncome", 3500.0,
                        "bikeId", "BIKE-900",
                        "bikeModel", "Gravel Explorer 900");
        ApplicationId applicationId = new ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        SubmitLeasingRequestUseCase.Command expectedCommand =
                new SubmitLeasingRequestUseCase.Command(
                        new CustomerName("John Doe"),
                        new Email("john.doe@test.com"),
                        35,
                        3500.0,
                        new BikeId("BIKE-900"),
                        "Gravel Explorer 900");
        when(useCase.submit(any())).thenReturn(applicationId);
        RequestBuilder operation =
                post("/api/bike-leasing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(input));

        // when: the request is performed
        MvcResult response = mockMvc.perform(operation).andReturn();

        // then: the use case is invoked with the mapped command and the id is returned
        assertThat(response.getResponse().getStatus()).isEqualTo(200);
        assertThat(response.getResponse().getContentAsString()).contains(applicationId.value().toString());
        verify(useCase).submit(expectedCommand);
        verifyNoMoreInteractions(useCase);
    }
}
