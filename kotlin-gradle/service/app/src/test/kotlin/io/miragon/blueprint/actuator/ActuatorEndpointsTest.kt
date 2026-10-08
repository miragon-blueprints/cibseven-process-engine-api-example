package io.miragon.blueprint.actuator

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Smoke-tests the operational surface added for issue #10: the actuator probes and the Prometheus
 * scrape endpoint must respond so orchestrators (liveness/readiness) and monitoring can rely on them.
 *
 * Boots a full servlet context the same way [io.miragon.blueprint.openapi.OpenApiSpecExportTest]
 * does — the `test` profile is `web-application-type=none`, so we override it to `servlet` and let
 * the H2 datasource back the built-in `db` health indicator (which reports `UP`).
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["spring.main.web-application-type=servlet"],
)
@ActiveProfiles("test")
class ActuatorEndpointsTest {

    @Value("\${local.server.port}")
    private var port: Int = 0

    @Test
    fun `health endpoint reports UP`() {
        val response = get("/actuator/health")
        assertThat(response.statusCode()).isEqualTo(200)
        assertThat(response.body()).contains("\"status\":\"UP\"")
    }

    @Test
    fun `liveness probe responds`() {
        val response = get("/actuator/health/liveness")
        assertThat(response.statusCode()).isEqualTo(200)
        assertThat(response.body()).contains("\"status\":\"UP\"")
    }

    @Test
    fun `readiness probe responds`() {
        val response = get("/actuator/health/readiness")
        assertThat(response.statusCode()).isEqualTo(200)
        assertThat(response.body()).contains("\"status\":\"UP\"")
    }

    @Test
    fun `prometheus endpoint exposes metrics`() {
        val response = get("/actuator/prometheus")
        assertThat(response.statusCode()).isEqualTo(200)
        assertThat(response.body()).contains("# HELP")
    }

    private fun get(path: String): HttpResponse<String> {
        val request = HttpRequest.newBuilder(URI.create("http://localhost:$port$path")).GET().build()
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())
    }
}
