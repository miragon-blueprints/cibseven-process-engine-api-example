package io.miragon.blueprint.application.service;

import static io.miragon.blueprint.domain.leasing.TestObjectBuilder.testLeasingApplication;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.miragon.blueprint.application.port.outbound.BikePortfolioRepository;
import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository;
import io.miragon.blueprint.application.port.outbound.TaskInboxPort;
import io.miragon.blueprint.domain.bike.Bike;
import io.miragon.blueprint.domain.bike.BikeId;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import io.miragon.blueprint.domain.leasing.LeasingApplication;
import io.miragon.blueprint.domain.leasing.PendingClarification;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetPendingClarificationsServiceTest {

    private final TaskInboxPort taskInbox = mock(TaskInboxPort.class);
    private final LeasingApplicationRepository repository = mock(LeasingApplicationRepository.class);
    private final BikePortfolioRepository bikePortfolio = mock(BikePortfolioRepository.class);
    private final GetPendingClarificationsService underTest =
            new GetPendingClarificationsService(taskInbox, repository, bikePortfolio);

    private final ApplicationId id = new ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
    private final LocalDateTime waitingSince = LocalDateTime.of(2026, 8, 17, 9, 30);

    @Test
    void enriches_each_open_task_with_its_application_and_requested_bike_model() {
        // given: one open clarification whose application requested BIKE-OOS
        LeasingApplication application = testLeasingApplication().id(id).bikeId(new BikeId("BIKE-OOS")).build();
        when(taskInbox.findOpenClarifications())
                .thenReturn(List.of(new TaskInboxPort.OpenClarification(id, waitingSince)));
        when(repository.findById(id)).thenReturn(Optional.of(application));
        when(bikePortfolio.findAllByIds(List.of(new BikeId("BIKE-OOS"))))
                .thenReturn(List.of(new Bike(new BikeId("BIKE-OOS"), "Mountain Trail 600")));

        // when: the inbox is read
        List<PendingClarification> result = underTest.pending();

        // then: the pending clarification carries who, which bike (+ model) and since when — but no task id
        assertThat(result).hasSize(1);
        PendingClarification clarification = result.getFirst();
        assertThat(clarification.applicationId()).isEqualTo(id);
        assertThat(clarification.customerName()).isEqualTo(application.customerName());
        assertThat(clarification.requestedBikeId()).isEqualTo(new BikeId("BIKE-OOS"));
        assertThat(clarification.requestedBikeModel()).isEqualTo("Mountain Trail 600");
        assertThat(clarification.waitingSince()).isEqualTo(waitingSince);
        // and: only the expected reads happened — a read-only query writes nothing
        verify(taskInbox).findOpenClarifications();
        verify(repository).findById(id);
        verify(bikePortfolio).findAllByIds(List.of(new BikeId("BIKE-OOS")));
        verifyNoMoreInteractions(taskInbox, repository, bikePortfolio);
    }

    @Test
    void skips_tasks_whose_application_can_no_longer_be_found() {
        // given: an open task pointing at a vanished application
        when(taskInbox.findOpenClarifications())
                .thenReturn(List.of(new TaskInboxPort.OpenClarification(id, waitingSince)));
        when(repository.findById(id)).thenReturn(Optional.empty());
        when(bikePortfolio.findAllByIds(List.of())).thenReturn(List.of());

        // when / then: the orphan task is dropped rather than surfacing a half-built row
        assertThat(underTest.pending()).isEmpty();
        verify(bikePortfolio).findAllByIds(List.of());
        verify(taskInbox).findOpenClarifications();
        verify(repository).findById(id);
        verifyNoMoreInteractions(taskInbox, repository, bikePortfolio);
    }

    @Test
    void returns_nothing_when_no_clarification_is_open() {
        // given: an empty task list
        when(taskInbox.findOpenClarifications()).thenReturn(List.of());
        when(bikePortfolio.findAllByIds(List.of())).thenReturn(List.of());

        // when / then: the inbox is empty
        assertThat(underTest.pending()).isEmpty();
        verifyNoInteractions(repository);
        verify(taskInbox).findOpenClarifications();
        verify(bikePortfolio).findAllByIds(List.of());
        verifyNoMoreInteractions(taskInbox, bikePortfolio);
    }
}
