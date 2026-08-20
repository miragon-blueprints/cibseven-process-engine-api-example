package io.miragon.blueprint.adapter.outbound.db

import io.miragon.blueprint.application.port.outbound.LeasingApplicationRepository
import io.miragon.blueprint.domain.leasing.ApplicationId
import io.miragon.blueprint.domain.leasing.LeasingStatus
import io.miragon.blueprint.domain.leasing.testLeasingApplication
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import java.util.UUID

@DataJpaTest
@ActiveProfiles("test")
@Import(LeasingApplicationPersistenceAdapter::class)
class LeasingApplicationPersistenceAdapterTest {

    @Autowired
    private lateinit var underTest: LeasingApplicationPersistenceAdapter

    @Autowired
    private lateinit var entityManager: TestEntityManager

    private val id = ApplicationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))

    @Test
    fun `saves and reloads an application`() {

        // given: a leasing application
        val application = testLeasingApplication(id = id)

        // when: it is saved and re-read from a cleared persistence context
        underTest.save(application)
        entityManager.flush()
        entityManager.clear()

        // then: the reloaded application equals the original
        assertThat(underTest.findById(id)).usingRecursiveComparison().isEqualTo(application)
    }

    @Test
    fun `findById returns null when the application does not exist`() {

        // given: an empty database
        // when / then: the lookup returns null
        assertThat(underTest.findById(id)).isNull()
    }

    @Test
    @Sql("classpath:sql/leasing-application.sql")
    fun `findById maps an existing row back to the domain`() {

        // given: a pre-inserted row (see sql/leasing-application.sql)
        // when / then: the adapter maps it back to the expected application
        assertThat(underTest.findById(id))
            .usingRecursiveComparison()
            .isEqualTo(testLeasingApplication(id = id))
    }

    @Test
    fun `findAll returns a page ordered by creation date, newest first`() {

        // given: three applications created on different days
        val older = testLeasingApplication(id = ApplicationId(UUID.randomUUID()), createdAt = java.time.LocalDateTime.of(2026, 1, 1, 8, 0))
        val newer = testLeasingApplication(id = ApplicationId(UUID.randomUUID()), createdAt = java.time.LocalDateTime.of(2026, 3, 1, 8, 0))
        val newest = testLeasingApplication(id = ApplicationId(UUID.randomUUID()), createdAt = java.time.LocalDateTime.of(2026, 6, 1, 8, 0))
        listOf(older, newest, newer).forEach { underTest.save(it) }
        entityManager.flush()
        entityManager.clear()

        // when: the first page is read without a filter
        val page = underTest.findAll(LeasingApplicationRepository.Criteria(status = null, page = 0, size = 10))

        // then: all three come back, newest first
        assertThat(page.totalElements).isEqualTo(3)
        assertThat(page.items.map { it.id }).containsExactly(newest.id, newer.id, older.id)
    }

    @Test
    fun `findAll filters by status and paginates`() {

        // given: two RECEIVED and one ACTIVE application
        underTest.save(testLeasingApplication(id = ApplicationId(UUID.randomUUID()), status = LeasingStatus.RECEIVED))
        underTest.save(testLeasingApplication(id = ApplicationId(UUID.randomUUID()), status = LeasingStatus.RECEIVED))
        underTest.save(testLeasingApplication(id = ApplicationId(UUID.randomUUID()), status = LeasingStatus.ACTIVE))
        entityManager.flush()
        entityManager.clear()

        // when: only ACTIVE applications are requested
        val page = underTest.findAll(LeasingApplicationRepository.Criteria(status = LeasingStatus.ACTIVE, page = 0, size = 10))

        // then: only the single ACTIVE application is returned
        assertThat(page.totalElements).isEqualTo(1)
        assertThat(page.items).hasSize(1)
        assertThat(page.items.single().status).isEqualTo(LeasingStatus.ACTIVE)
    }
}
