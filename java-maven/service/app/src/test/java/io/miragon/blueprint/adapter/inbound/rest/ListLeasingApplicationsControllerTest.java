package io.miragon.blueprint.adapter.inbound.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import io.miragon.blueprint.application.port.inbound.ListLeasingApplicationsQuery;
import io.miragon.blueprint.domain.bike.BikeId;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import io.miragon.blueprint.domain.leasing.CustomerName;
import io.miragon.blueprint.domain.leasing.LeasingStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(ListLeasingApplicationsController.class)
class ListLeasingApplicationsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListLeasingApplicationsQuery query;

    private final ApplicationId id = new ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));

    @Test
    void returns_a_page_of_application_summaries() throws Exception {
        // given: a single-item page
        when(query.list(any())).thenReturn(
                new ListLeasingApplicationsQuery.Page(
                        List.of(
                                new ListLeasingApplicationsQuery.Item(
                                        id,
                                        new CustomerName("Alice Rider"),
                                        new BikeId("BIKE-900"),
                                        "Gravel Explorer 900",
                                        LeasingStatus.RECEIVED,
                                        LocalDateTime.of(2026, 8, 17, 10, 30))),
                        0,
                        20,
                        1,
                        1));

        // when
        MvcResult response = mockMvc.perform(get("/api/bike-leasing")).andReturn();

        // then
        assertThat(response.getResponse().getStatus()).isEqualTo(200);
        assertThat(response.getResponse().getContentAsString()).contains(
                id.value().toString(),
                "Alice Rider",
                "BIKE-900",
                "Gravel Explorer 900",
                "RECEIVED",
                "\"totalElements\":1");
    }

    @Test
    void parses_the_status_filter_and_paging_params_into_the_query() throws Exception {
        // given
        when(query.list(any())).thenReturn(new ListLeasingApplicationsQuery.Page(List.of(), 2, 5, 0, 0));
        ArgumentCaptor<ListLeasingApplicationsQuery.Filter> filter =
                ArgumentCaptor.forClass(ListLeasingApplicationsQuery.Filter.class);

        // when: a filtered request is made with lowercase status
        mockMvc.perform(get("/api/bike-leasing?status=active&page=2&size=5")).andReturn();

        // then: the parsed filter reaches the query
        verify(query).list(filter.capture());
        assertThat(filter.getValue().status()).isEqualTo(LeasingStatus.ACTIVE);
        assertThat(filter.getValue().page()).isEqualTo(2);
        assertThat(filter.getValue().size()).isEqualTo(5);
    }

    @Test
    void rejects_an_unknown_status_with_a_400_problem_detail() throws Exception {
        // when: an invalid status is requested
        MvcResult response = mockMvc.perform(get("/api/bike-leasing?status=bogus")).andReturn();

        // then: the global advice turns it into an RFC 9457 problem response
        assertThat(response.getResponse().getStatus()).isEqualTo(400);
        assertThat(response.getResponse().getContentType()).contains("application/problem+json");
        assertThat(response.getResponse().getContentAsString()).contains("unknown status 'bogus'");
    }
}
