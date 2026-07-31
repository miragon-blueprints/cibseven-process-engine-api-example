package io.miragon.blueprint.adapter.outbound.db

import io.miragon.blueprint.domain.bike.Bike
import io.miragon.blueprint.domain.bike.BikeId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@ActiveProfiles("test")
@Import(BikePortfolioPersistenceAdapter::class)
class BikePortfolioPersistenceAdapterTest {

    @Autowired
    private lateinit var underTest: BikePortfolioPersistenceAdapter

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Test
    fun `saves and reloads a bike`() {

        // given: a bike in the portfolio
        val bike = Bike(BikeId("BIKE-900"), "Gravel Explorer 900")

        // when: it is saved and re-read from a cleared persistence context
        underTest.save(bike)
        entityManager.flush()
        entityManager.clear()

        // then: the reloaded bike equals the original
        assertThat(underTest.findByBikeId(BikeId("BIKE-900"))).isEqualTo(bike)
    }

    @Test
    fun `findByBikeId returns null when the bike is not in the portfolio`() {

        // given: an empty portfolio
        // when / then: the lookup returns null
        assertThat(underTest.findByBikeId(BikeId("BIKE-000"))).isNull()
    }
}
