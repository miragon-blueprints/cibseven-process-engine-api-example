package io.miragon.blueprint.adapter.inbound.rest;

import static io.miragon.blueprint.domain.leasing.TestObjectBuilder.testLeasingApplication;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import io.miragon.blueprint.application.port.inbound.GetLeasingApplicationQuery;
import io.miragon.blueprint.domain.bike.OrderId;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import io.miragon.blueprint.domain.leasing.ContractId;
import io.miragon.blueprint.domain.leasing.LeasingApplication;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;

@WebMvcTest(GetLeasingApplicationController.class)
class GetLeasingApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetLeasingApplicationQuery query;

    @Test
    void returns_the_application_with_all_of_its_fields_mapped_when_it_exists() throws Exception {

        // given: a fully-populated application (incl. order and contract) the query can find
        LeasingApplication application = testLeasingApplication()
                .orderId(new OrderId("ORDER-42"))
                .contractId(new ContractId("CONTRACT-7"))
                .build();
        when(query.byId(application.id()))
                .thenReturn(Optional.of(new GetLeasingApplicationQuery.Result(application, "Gravel Explorer 900")));
        RequestBuilder operation = get("/api/bike-leasing/{applicationId}", application.id().value().toString());

        // when: the request is performed
        MvcResult response = mockMvc.perform(operation).andReturn();

        // then: the response is 200 and every DTO field is mapped from the domain object
        assertThat(response.getResponse().getStatus()).isEqualTo(200);
        assertThat(response.getResponse().getContentAsString()).contains(
                application.id().value().toString(),
                application.customerName().value(),
                application.email().value(),
                "\"age\":35",
                "\"monthlyNetIncome\":3500.0",
                application.bikeId().value(),
                "Gravel Explorer 900",
                "RECEIVED",
                "ORDER-42",
                "CONTRACT-7",
                "\"createdAt\":\"2024-01-15T10:30:00\"");
        verify(query).byId(application.id());
        verifyNoMoreInteractions(query);
    }

    @Test
    void maps_a_not_yet_ordered_application_with_null_order_and_contract() throws Exception {

        // given: an application still without an order or contract (both nullable fields absent)
        LeasingApplication application = testLeasingApplication().orderId(null).contractId(null).build();
        when(query.byId(application.id()))
                .thenReturn(Optional.of(new GetLeasingApplicationQuery.Result(application, null)));
        RequestBuilder operation = get("/api/bike-leasing/{applicationId}", application.id().value().toString());

        // when: the request is performed
        MvcResult response = mockMvc.perform(operation).andReturn();

        // then: the response is 200 and the nullable fields are serialised as null
        assertThat(response.getResponse().getStatus()).isEqualTo(200);
        assertThat(response.getResponse().getContentAsString())
                .contains("\"orderId\":null", "\"contractId\":null", "\"bikeModel\":null");
    }

    @Test
    void returns_404_when_the_application_does_not_exist() throws Exception {

        // given: an unknown application id
        ApplicationId id = ApplicationId.of("123e4567-e89b-12d3-a456-426614174000");
        when(query.byId(id)).thenReturn(Optional.empty());
        RequestBuilder operation = get("/api/bike-leasing/{applicationId}", id.value().toString());

        // when: the request is performed
        MvcResult response = mockMvc.perform(operation).andReturn();

        // then: the response is 404 Not Found
        assertThat(response.getResponse().getStatus()).isEqualTo(404);
    }
}
