package io.miragon.blueprint.application.service;

import static io.miragon.blueprint.domain.leasing.TestObjectBuilder.testLeasingApplication;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.miragon.blueprint.application.port.inbound.ListLeasingApplicationsQuery;
import io.miragon.blueprint.application.port.outbound.BikePortfolioRepository;
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository;
import io.miragon.blueprint.domain.bike.Bike;
import io.miragon.blueprint.domain.bike.BikeId;
import io.miragon.blueprint.domain.leasing.LeasingApplication;
import io.miragon.blueprint.domain.leasing.LeasingStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class ListLeasingApplicationsServiceTest {

    private final LeasingApplicationRepository repository = mock(LeasingApplicationRepository.class);
    private final BikePortfolioRepository bikePortfolio = mock(BikePortfolioRepository.class);
    private final ListLeasingApplicationsService underTest =
            new ListLeasingApplicationsService(repository, bikePortfolio);

    @Test
    void maps_a_page_of_applications_and_resolves_each_bike_model_in_one_batch_query() {
        // given: a page with two applications on different bikes
        LeasingApplication a = testLeasingApplication()
                .bikeId(new BikeId("BIKE-900"))
                .status(LeasingStatus.RECEIVED)
                .build();
        LeasingApplication b = testLeasingApplication()
                .bikeId(new BikeId("BIKE-800"))
                .status(LeasingStatus.ACTIVE)
                .build();
        when(repository.findAll(any()))
                .thenReturn(new LeasingApplicationRepository.Page(List.of(a, b), 0, 20, 2, 1));
        when(bikePortfolio.findAllByIds(any())).thenReturn(List.of(
                new Bike(new BikeId("BIKE-900"), "Gravel Explorer 900"),
                new Bike(new BikeId("BIKE-800"), "Carbon Road 800")));

        // when: the first page is requested without a status filter
        ListLeasingApplicationsQuery.Page result =
                underTest.list(new ListLeasingApplicationsQuery.Filter(null, 0, 20));

        // then: paging metadata is preserved and each item carries its resolved model
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(0).bikeModel()).isEqualTo("Gravel Explorer 900");
        assertThat(result.items().get(1).bikeModel()).isEqualTo("Carbon Road 800");
        // and: exactly one batch lookup was issued for the bikes, not one per row
        verify(bikePortfolio, times(1)).findAllByIds(List.of(new BikeId("BIKE-900"), new BikeId("BIKE-800")));
        verify(repository).findAll(new LeasingApplicationRepository.Criteria(null, 0, 20));
        verifyNoMoreInteractions(repository, bikePortfolio);
    }

    @Test
    void passes_the_status_filter_through_to_the_repository_criteria() {
        // given: a repository that records the criteria it was asked for
        when(repository.findAll(any()))
                .thenReturn(new LeasingApplicationRepository.Page(List.of(), 1, 5, 0, 0));
        when(bikePortfolio.findAllByIds(List.of())).thenReturn(List.of());

        // when: a filtered, paged request is made
        underTest.list(new ListLeasingApplicationsQuery.Filter(LeasingStatus.ACTIVE, 1, 5));

        // then: the exact criteria reach the outbound port
        verify(repository).findAll(new LeasingApplicationRepository.Criteria(LeasingStatus.ACTIVE, 1, 5));
        verify(bikePortfolio).findAllByIds(List.of());
        verifyNoMoreInteractions(repository, bikePortfolio);
    }

    @Test
    void leaves_the_model_null_when_the_bike_is_not_in_the_portfolio() {
        // given: an application whose bike is missing from the portfolio
        LeasingApplication orphan = testLeasingApplication().bikeId(new BikeId("BIKE-GONE")).build();
        when(repository.findAll(any()))
                .thenReturn(new LeasingApplicationRepository.Page(List.of(orphan), 0, 20, 1, 1));
        when(bikePortfolio.findAllByIds(any())).thenReturn(List.of());

        // when: the page is mapped
        ListLeasingApplicationsQuery.Page result =
                underTest.list(new ListLeasingApplicationsQuery.Filter(null, 0, 20));

        // then: the missing model surfaces as null rather than throwing
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).bikeModel()).isNull();
        verify(repository).findAll(new LeasingApplicationRepository.Criteria(null, 0, 20));
        verify(bikePortfolio).findAllByIds(List.of(new BikeId("BIKE-GONE")));
        verifyNoMoreInteractions(repository, bikePortfolio);
    }
}
