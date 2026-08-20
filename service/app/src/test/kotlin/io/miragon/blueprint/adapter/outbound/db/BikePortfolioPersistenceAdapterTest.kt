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

    @Test
    fun `findAll returns the whole catalogue ordered by id`() {

        // given: two bikes saved out of id order
        underTest.save(Bike(BikeId("BIKE-900"), "Gravel Explorer 900"))
        underTest.save(Bike(BikeId("BIKE-800"), "Carbon Road 800"))
        entityManager.flush()
        entityManager.clear()

        // when / then: both are returned, ordered ascending by id
        assertThat(underTest.findAll())
            .containsExactly(
                Bike(BikeId("BIKE-800"), "Carbon Road 800"),
                Bike(BikeId("BIKE-900"), "Gravel Explorer 900"),
            )
    }

    @Test
    fun `findAllByIds resolves only the requested bikes and short-circuits on empty input`() {

        // given: three bikes
        underTest.save(Bike(BikeId("BIKE-900"), "Gravel Explorer 900"))
        underTest.save(Bike(BikeId("BIKE-800"), "Carbon Road 800"))
        underTest.save(Bike(BikeId("BIKE-OOS"), "Mountain Trail 600"))
        entityManager.flush()
        entityManager.clear()

        // when: two of them are looked up by id
        val result = underTest.findAllByIds(listOf(BikeId("BIKE-900"), BikeId("BIKE-OOS")))

        // then: exactly those two come back
        assertThat(result.map { it.bikeId.value }).containsExactlyInAnyOrder("BIKE-900", "BIKE-OOS")
        // and: an empty request returns an empty list without touching the database
        assertThat(underTest.findAllByIds(emptyList())).isEmpty()
    }
}
