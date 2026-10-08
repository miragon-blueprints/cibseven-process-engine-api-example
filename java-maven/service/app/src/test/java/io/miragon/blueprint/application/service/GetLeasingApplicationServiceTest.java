package io.miragon.blueprint.application.service;

import static io.miragon.blueprint.domain.leasing.TestObjectBuilder.testLeasingApplication;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.miragon.blueprint.application.port.inbound.GetLeasingApplicationQuery;
import io.miragon.blueprint.application.port.outbound.BikePortfolioRepository;
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository;
import io.miragon.blueprint.domain.bike.Bike;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import io.miragon.blueprint.domain.leasing.LeasingApplication;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetLeasingApplicationServiceTest {

    private final LeasingApplicationRepository repository = mock(LeasingApplicationRepository.class);
    private final BikePortfolioRepository bikePortfolio = mock(BikePortfolioRepository.class);
    private final GetLeasingApplicationService underTest =
            new GetLeasingApplicationService(repository, bikePortfolio);

    @Test
    void byId_returns_the_application_with_its_bike_model_resolved_from_the_portfolio() {

        // given: an application and its bike in the portfolio
        LeasingApplication application = testLeasingApplication().build();
        when(repository.findById(application.id())).thenReturn(Optional.of(application));
        when(bikePortfolio.findByBikeId(application.bikeId()))
                .thenReturn(Optional.of(new Bike(application.bikeId(), "Gravel Explorer 900")));

        // when: the application is queried by id
        Optional<GetLeasingApplicationQuery.Result> result = underTest.byId(application.id());

        // then: the application and the resolved bike model are returned
        assertThat(result).isPresent();
        assertThat(result.get().application()).isEqualTo(application);
        assertThat(result.get().bikeModel()).isEqualTo("Gravel Explorer 900");
        verify(repository).findById(application.id());
        verify(bikePortfolio).findByBikeId(application.bikeId());
        verifyNoMoreInteractions(repository, bikePortfolio);
    }

    @Test
    void byId_returns_null_when_the_application_does_not_exist() {

        // given: an unknown application id
        ApplicationId id = new ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        when(repository.findById(id)).thenReturn(Optional.empty());

        // when / then: the query returns nothing (an empty Optional) and the portfolio is never consulted
        assertThat(underTest.byId(id)).isEmpty();
        verify(repository).findById(id);
        verifyNoMoreInteractions(repository, bikePortfolio);
    }
}
