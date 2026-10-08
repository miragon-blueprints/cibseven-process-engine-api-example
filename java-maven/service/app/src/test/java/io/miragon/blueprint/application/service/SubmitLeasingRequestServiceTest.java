package io.miragon.blueprint.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.miragon.blueprint.application.port.inbound.SubmitLeasingRequestUseCase;
import io.miragon.blueprint.application.port.outbound.BikePortfolioRepository;
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository;
import io.miragon.blueprint.application.port.outbound.LeasingProcess;
import io.miragon.blueprint.domain.bike.Bike;
import io.miragon.blueprint.domain.bike.BikeId;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import io.miragon.blueprint.domain.leasing.CustomerName;
import io.miragon.blueprint.domain.leasing.Email;
import io.miragon.blueprint.domain.leasing.LeasingStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class SubmitLeasingRequestServiceTest {

    private final LeasingApplicationRepository repository = mock(LeasingApplicationRepository.class);
    private final BikePortfolioRepository bikePortfolio = mock(BikePortfolioRepository.class);
    private final LeasingProcess process = mock(LeasingProcess.class);
    private final Clock clock = Clock.fixed(Instant.parse("2024-01-15T10:30:00Z"), ZoneOffset.UTC);
    private final SubmitLeasingRequestService underTest =
            new SubmitLeasingRequestService(repository, bikePortfolio, process, clock);

    @Test
    void submit_registers_the_bike_persists_a_received_application_and_starts_the_process() {

        // given: a leasing-request command and stubbed out-ports
        SubmitLeasingRequestUseCase.Command command =
                new SubmitLeasingRequestUseCase.Command(
                        new CustomerName("John Doe"),
                        new Email("john.doe@test.com"),
                        35,
                        3500.0,
                        new BikeId("BIKE-900"),
                        "Gravel Explorer 900");
        when(bikePortfolio.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // when: the use case is invoked
        ApplicationId id = underTest.submit(command);

        // then: the bike is stored in the portfolio, a RECEIVED application referencing it is saved, and the process starts
        verify(bikePortfolio).save(new Bike(new BikeId("BIKE-900"), "Gravel Explorer 900"));
        verify(repository).save(argThat(a ->
                a.id().equals(id)
                        && a.status() == LeasingStatus.RECEIVED
                        && a.bikeId().equals(new BikeId("BIKE-900"))
                        && a.createdAt().equals(LocalDateTime.now(clock))));
        verify(process).submitRequest(argThat(a -> a.id().equals(id)));
        verifyNoMoreInteractions(repository, bikePortfolio, process);
    }
}
