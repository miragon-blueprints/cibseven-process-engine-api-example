package io.miragon.blueprint.openapi

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import tools.jackson.core.util.DefaultIndenter
import tools.jackson.core.util.DefaultPrettyPrinter
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.ObjectNode
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

/**
 * Exports the live OpenAPI contract to `openapi/openapi.json` at the repo root, so the committed
 * spec can never lie about the code. CI regenerates it and runs `git diff --exit-code` — the
 * output must be byte-for-byte deterministic, or that gate would flap.
 *
 * Determinism is bought three ways: [SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS] sorts every
 * object key, a fixed two-space LF indenter keeps it stable across OSes, and a trailing newline
 * keeps POSIX tools happy.
 *
 * This is not really an assertion test — it is a code generator wearing a JUnit costume so it runs
 * inside `./gradlew build` with a live application context. See ADR-0003
 * (docs/adr/0003-openapi-as-the-checked-in-contract.md).
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["spring.main.web-application-type=servlet"],
)
@ActiveProfiles("test")
class OpenApiSpecExportTest {

    @Value("\${local.server.port}")
    private var port: Int = 0

    private val deterministicMapper =
        JsonMapper.builder()
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .defaultPrettyPrinter(
                DefaultPrettyPrinter().withObjectIndenter(DefaultIndenter("  ", "\n")),
            )
            .build()

    @Test
    fun `exports the OpenAPI contract to openapi_openapi_json at the repo root`() {
        // given: the live spec served by springdoc
        val raw = fetch("http://localhost:$port/v3/api-docs")
        assertThat(raw).isNotBlank()

        // when: it is re-serialised with sorted keys and a fixed indenter
        val tree = deterministicMapper.readTree(raw) as ObjectNode
        // Drop the `servers` block — springdoc fills it with the random test port, which would make
        // the drift gate flap. An API consumer resolves the base URL from its own configuration anyway.
        tree.remove("servers")
        val pretty = deterministicMapper.writeValueAsString(tree) + "\n"

        // then: the result contains our /api paths and is written to the committed location
        assertThat(pretty).contains("\"/api/bike-leasing\"")
        val target = repoRoot().resolve("openapi").resolve("openapi.json")
        Files.createDirectories(target.parent)
        Files.writeString(target, pretty)
    }

    private fun fetch(url: String): String {
        val request = HttpRequest.newBuilder(URI.create(url)).GET().build()
        val response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())
        assertThat(response.statusCode()).isEqualTo(200)
        return response.body()
    }

    /** Walk up from the module working directory until the file with `settings.gradle.kts` is found. */
    private fun repoRoot(): Path {
        var dir: Path? = Path.of(System.getProperty("user.dir")).toAbsolutePath()
        while (dir != null) {
            if (dir.resolve("settings.gradle.kts").exists()) return dir
            dir = dir.parent
        }
        error("could not locate the repo root (no settings.gradle.kts found above ${System.getProperty("user.dir")})")
    }
}
