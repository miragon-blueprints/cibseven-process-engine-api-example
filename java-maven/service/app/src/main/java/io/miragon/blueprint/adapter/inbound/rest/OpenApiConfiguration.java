package io.miragon.blueprint.adapter.inbound.rest;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * OpenAPI metadata for the generated contract served at {@code /v3/api-docs}.
 *
 * <p>This is cross-cutting web configuration and lives in {@code adapter.inbound.rest}, NOT in a separate
 * {@code config} package — the architecture tests ignore only direct members of the root package, so a
 * new {@code io.miragon.blueprint.config} package would fail the suite. The {@code Configuration} suffix is
 * whitelisted for this package in {@code NamingConventionArchitectureTest}.
 */
@Configuration
public class OpenApiConfiguration {

    // The CIB seven webapp registers its own OpenAPI bean (org.cibseven.webapp.config.OpenApiConfig).
    // @Primary makes springdoc's openAPIBuilder pick ours for the /api/** contract any API consumer
    // is generated from.
    @Bean
    @Primary
    public OpenAPI bikeLeasingOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("MiraVelo Bike-Leasing API")
                        .version("1.0")
                        .description(
                                "Customer-portal and back-office endpoints for the MiraVelo bike-leasing "
                                        + "process. The engine-internal /engine-rest API is intentionally excluded."));
    }
}
