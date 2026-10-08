package io.miragon.blueprint

import io.miragon.blueprint.application.port.outbound.BikePortfolioRepository
import io.miragon.blueprint.domain.bike.Bike
import io.miragon.blueprint.domain.bike.BikeId
import mu.KotlinLogging
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

/**
 * Seeds MiraVelo's bike catalogue on start-up so the `GET /api/bikes` picker is never empty and the
 * Bruno scenarios always find their ids. Idempotent — `save` upserts by id.
 *
 * `BIKE-OOS` is seeded deliberately: it is the dealer's out-of-stock bike, so selecting it from the
 * UI drives the bike-unavailable → alternative-selection scenario end to end. Availability itself is
 * decided by the dealer, not stored here.
 *
 * It lives in the root package next to the application class — application bootstrap, outside the
 * hexagonal layers — and is excluded from mutation testing for the same reason.
 */
@Component
class BikeCatalogueSeeder(
    private val bikePortfolio: BikePortfolioRepository,
) : ApplicationRunner {

    private val log = KotlinLogging.logger {}

    override fun run(args: ApplicationArguments) {
        CATALOGUE.forEach { bikePortfolio.save(it) }
        log.info { "Seeded ${CATALOGUE.size} bikes into the portfolio" }
    }

    private companion object {
        val CATALOGUE =
            listOf(
                Bike(BikeId("BIKE-900"), "Gravel Explorer 900"),
                Bike(BikeId("BIKE-800"), "Carbon Road 800"),
                Bike(BikeId("BIKE-OOS"), "Mountain Trail 600"),
            )
    }
}
