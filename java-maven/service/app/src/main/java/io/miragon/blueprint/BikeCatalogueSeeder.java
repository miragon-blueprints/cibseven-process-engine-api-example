package io.miragon.blueprint;

import io.miragon.blueprint.application.port.outbound.BikePortfolioRepository;
import io.miragon.blueprint.domain.bike.Bike;
import io.miragon.blueprint.domain.bike.BikeId;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds MiraVelo's bike catalogue on start-up so the {@code GET /api/bikes} picker is never empty and the
 * Bruno scenarios always find their ids. Idempotent — {@code save} upserts by id.
 *
 * <p>{@code BIKE-OOS} is seeded deliberately: it is the dealer's out-of-stock bike, so selecting it from the
 * UI drives the bike-unavailable → alternative-selection scenario end to end. Availability itself is
 * decided by the dealer, not stored here.
 *
 * <p>It lives in the root package next to the application class — application bootstrap, outside the
 * hexagonal layers — and is excluded from mutation testing for the same reason.
 */
@Component
public class BikeCatalogueSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BikeCatalogueSeeder.class);

    private static final List<Bike> CATALOGUE =
            List.of(
                    new Bike(new BikeId("BIKE-900"), "Gravel Explorer 900"),
                    new Bike(new BikeId("BIKE-800"), "Carbon Road 800"),
                    new Bike(new BikeId("BIKE-OOS"), "Mountain Trail 600"));

    private final BikePortfolioRepository bikePortfolio;

    public BikeCatalogueSeeder(BikePortfolioRepository bikePortfolio) {
        this.bikePortfolio = bikePortfolio;
    }

    @Override
    public void run(ApplicationArguments args) {
        CATALOGUE.forEach(bikePortfolio::save);
        log.info("Seeded {} bikes into the portfolio", CATALOGUE.size());
    }
}
