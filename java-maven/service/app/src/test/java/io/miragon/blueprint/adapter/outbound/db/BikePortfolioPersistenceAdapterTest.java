package io.miragon.blueprint.adapter.outbound.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.miragon.blueprint.domain.bike.Bike;
import io.miragon.blueprint.domain.bike.BikeId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@DataJpaTest
@ActiveProfiles("test")
@Import(BikePortfolioPersistenceAdapter.class)
class BikePortfolioPersistenceAdapterTest {

    @Autowired
    private BikePortfolioPersistenceAdapter underTest;

    @Autowired
    private TestEntityManager entityManager;

    @MockitoSpyBean
    private BikePortfolioJpaRepository jpaRepository;

    @Test
    void saves_and_reloads_a_bike() {

        // given: a bike in the portfolio
        Bike bike = new Bike(new BikeId("BIKE-900"), "Gravel Explorer 900");

        // when: it is saved and re-read from a cleared persistence context
        Bike saved = underTest.save(bike);
        entityManager.flush();
        entityManager.clear();

        // then: the reloaded bike equals the original
        assertThat(underTest.findByBikeId(new BikeId("BIKE-900"))).contains(bike);
        // and: save hands back the persisted bike
        assertThat(saved).isEqualTo(bike);
    }

    @Test
    void findByBikeId_returns_null_when_the_bike_is_not_in_the_portfolio() {

        // given: an empty portfolio
        // when / then: the lookup finds nothing ("null" in the test name = an empty Optional)
        assertThat(underTest.findByBikeId(new BikeId("BIKE-000"))).isEmpty();
    }

    @Test
    void findAll_returns_the_whole_catalogue_ordered_by_id() {

        // given: two bikes saved out of id order
        underTest.save(new Bike(new BikeId("BIKE-900"), "Gravel Explorer 900"));
        underTest.save(new Bike(new BikeId("BIKE-800"), "Carbon Road 800"));
        entityManager.flush();
        entityManager.clear();

        // when / then: both are returned, ordered ascending by id
        assertThat(underTest.findAll())
                .containsExactly(
                        new Bike(new BikeId("BIKE-800"), "Carbon Road 800"),
                        new Bike(new BikeId("BIKE-900"), "Gravel Explorer 900"));
    }

    @Test
    void findAllByIds_resolves_only_the_requested_bikes_and_short_circuits_on_empty_input() {

        // given: three bikes
        underTest.save(new Bike(new BikeId("BIKE-900"), "Gravel Explorer 900"));
        underTest.save(new Bike(new BikeId("BIKE-800"), "Carbon Road 800"));
        underTest.save(new Bike(new BikeId("BIKE-OOS"), "Mountain Trail 600"));
        entityManager.flush();
        entityManager.clear();

        // when: two of them are looked up by id
        List<Bike> result = underTest.findAllByIds(List.of(new BikeId("BIKE-900"), new BikeId("BIKE-OOS")));

        // then: exactly those two come back
        assertThat(result).extracting(bike -> bike.bikeId().value()).containsExactlyInAnyOrder("BIKE-900", "BIKE-OOS");
        // and: an empty request returns an empty list without touching the database
        assertThat(underTest.findAllByIds(List.of())).isEmpty();
        verify(jpaRepository, never()).findAllById(List.of());
    }
}
