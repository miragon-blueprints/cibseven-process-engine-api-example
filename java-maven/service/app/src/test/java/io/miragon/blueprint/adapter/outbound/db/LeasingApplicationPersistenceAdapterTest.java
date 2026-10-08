package io.miragon.blueprint.adapter.outbound.db;

import static io.miragon.blueprint.domain.leasing.TestObjectBuilder.testLeasingApplication;
import static org.assertj.core.api.Assertions.assertThat;

import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository;
import io.miragon.blueprint.domain.leasing.ApplicationId;
import io.miragon.blueprint.domain.leasing.LeasingApplication;
import io.miragon.blueprint.domain.leasing.LeasingStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@DataJpaTest
@ActiveProfiles("test")
@Import(LeasingApplicationPersistenceAdapter.class)
class LeasingApplicationPersistenceAdapterTest {

    @Autowired
    private LeasingApplicationPersistenceAdapter underTest;

    @Autowired
    private TestEntityManager entityManager;

    private final ApplicationId id = new ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));

    @Test
    void saves_and_reloads_an_application() {

        // given: a leasing application
        LeasingApplication application = testLeasingApplication().id(id).build();

        // when: it is saved and re-read from a cleared persistence context
        LeasingApplication saved = underTest.save(application);
        entityManager.flush();
        entityManager.clear();

        // then: the reloaded application equals the original
        assertThat(underTest.findById(id)).get().usingRecursiveComparison().isEqualTo(application);
        // and: save hands back the persisted application
        assertThat(saved).usingRecursiveComparison().isEqualTo(application);
    }

    @Test
    void findById_returns_null_when_the_application_does_not_exist() {

        // given: an empty database
        // when / then: the lookup finds nothing ("null" in the test name = an empty Optional)
        assertThat(underTest.findById(id)).isEmpty();
    }

    @Test
    @Sql("classpath:sql/leasing-application.sql")
    void findById_maps_an_existing_row_back_to_the_domain() {

        // given: a pre-inserted row (see sql/leasing-application.sql)
        // when / then: the adapter maps it back to the expected application
        assertThat(underTest.findById(id))
                .get()
                .usingRecursiveComparison()
                .isEqualTo(testLeasingApplication().id(id).build());
    }

    @Test
    void findAll_returns_a_page_ordered_by_creation_date_newest_first() {

        // given: three applications created on different days
        LeasingApplication older = testLeasingApplication()
                .id(new ApplicationId(UUID.randomUUID()))
                .createdAt(LocalDateTime.of(2026, 1, 1, 8, 0))
                .build();
        LeasingApplication newer = testLeasingApplication()
                .id(new ApplicationId(UUID.randomUUID()))
                .createdAt(LocalDateTime.of(2026, 3, 1, 8, 0))
                .build();
        LeasingApplication newest = testLeasingApplication()
                .id(new ApplicationId(UUID.randomUUID()))
                .createdAt(LocalDateTime.of(2026, 6, 1, 8, 0))
                .build();
        List.of(older, newest, newer).forEach(underTest::save);
        entityManager.flush();
        entityManager.clear();

        // when: the first page is read without a filter
        LeasingApplicationRepository.Page page =
                underTest.findAll(new LeasingApplicationRepository.Criteria(null, 0, 10));

        // then: all three come back, newest first
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.items()).extracting(LeasingApplication::id).containsExactly(newest.id(), newer.id(), older.id());
    }

    @Test
    void findAll_filters_by_status_and_paginates() {

        // given: two RECEIVED and one ACTIVE application
        underTest.save(testLeasingApplication()
                .id(new ApplicationId(UUID.randomUUID()))
                .status(LeasingStatus.RECEIVED)
                .build());
        underTest.save(testLeasingApplication()
                .id(new ApplicationId(UUID.randomUUID()))
                .status(LeasingStatus.RECEIVED)
                .build());
        underTest.save(testLeasingApplication()
                .id(new ApplicationId(UUID.randomUUID()))
                .status(LeasingStatus.ACTIVE)
                .build());
        entityManager.flush();
        entityManager.clear();

        // when: only ACTIVE applications are requested
        LeasingApplicationRepository.Page page =
                underTest.findAll(new LeasingApplicationRepository.Criteria(LeasingStatus.ACTIVE, 0, 10));

        // then: only the single ACTIVE application is returned
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.items()).hasSize(1);
        assertThat(page.items()).singleElement().extracting(LeasingApplication::status).isEqualTo(LeasingStatus.ACTIVE);
    }
}
