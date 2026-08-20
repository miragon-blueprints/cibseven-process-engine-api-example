package io.miragon.blueprint.adapter.inbound.rest

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Documented escape hatch you should NOT need. This blueprint is headless: its REST API is served
 * same-origin, so there is no CORS on the production path. This bean only activates under the `dev`
 * profile — if you ever point a browser-based API consumer at the backend cross-origin during
 * development, enable it. See CONTRIBUTING.md.
 */
@Configuration
@Profile("dev")
class DevCorsConfiguration : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry
            .addMapping("/api/**")
            .allowedOrigins("http://localhost:5173")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH")
    }
}
