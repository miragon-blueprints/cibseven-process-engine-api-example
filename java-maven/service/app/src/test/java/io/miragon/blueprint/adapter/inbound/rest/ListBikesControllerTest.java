package io.miragon.blueprint.adapter.inbound.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import io.miragon.blueprint.application.port.inbound.ListBikesQuery;
import io.miragon.blueprint.domain.bike.BikeId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(ListBikesController.class)
class ListBikesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListBikesQuery query;

    @Test
    void returns_the_catalogue_with_availability_flags() throws Exception {
        // given: a catalogue with one available and one out-of-stock bike
        when(query.all()).thenReturn(
                List.of(
                        new ListBikesQuery.Item(new BikeId("BIKE-900"), "Gravel Explorer 900", true),
                        new ListBikesQuery.Item(new BikeId("BIKE-OOS"), "Mountain Trail 600", false)));

        // when
        MvcResult response = mockMvc.perform(get("/api/bikes")).andReturn();

        // then: both bikes and their availability are serialised
        assertThat(response.getResponse().getStatus()).isEqualTo(200);
        assertThat(response.getResponse().getContentAsString()).contains(
                "BIKE-900",
                "Gravel Explorer 900",
                "BIKE-OOS",
                "\"available\":true",
                "\"available\":false");
    }
}
