# AGENTS.md

Guidance for AI agents (and humans) working in this repo. This is the real file; `CLAUDE.md` just
imports it.

## Project Overview

A headless **MiraVelo bike-leasing** example: one complete, runnable BPMN process automated with a
CIB seven embedded engine, driven through the **process-engine-api** (bpm-crafters) abstraction, with
an architecture enforced by tests.

- **Backend** (`service/app`) — Java 21 / Spring Boot 4, hexagonal, CIB seven 2.2.0 embedded engine,
  built with Maven (`./mvnw` wrapper, reactor root `pom.xml`). Package root `io.miragon.blueprint`.
- **Process-engine-api, not embedded delegates** — BPMN service tasks are `camunda:type="external"`
  topics consumed by `@ProcessEngineWorker` beans in `adapter/inbound/cibseven`; the engine is driven
  (start / correlate / complete) through `dev.bpmcrafters.processengineapi` in `adapter/outbound/cibseven`.
  There are **no** `JavaDelegate`/`ExecutionListener`/`TaskListener` and no `camunda:delegateExpression`.
- **The API contract** is `openapi/openapi.json`: springdoc generates it from the controllers, it is
  **committed and drift-gated** in CI so any REST change is caught. See ADR-0003.

## Development Setup

Two commands to a running backend (JDK 21; the `./mvnw` wrapper brings Maven):

```bash
docker compose -f stack/docker-compose.yml up -d   # Postgres
./mvnw -pl service/app -am spring-boot:run          # backend + embedded engine on :8080
```

### Ports (one source of truth — keep README, this file and `.conductor/settings.toml` in sync)

| What | Port |
|---|---|
| Postgres | 5432 |
| Backend (REST + engine-rest) | 8080 |
| CIB seven Cockpit / webapps | 8080/camunda (admin/admin) |
| OpenAPI spec · Swagger UI | 8080/v3/api-docs · 8080/swagger-ui.html |
| Actuator (health/liveness/readiness · prometheus) | 8080/actuator |

## Build Commands

| Area | Command |
|---|---|
| Backend (arch + unit + process + model validation + spec export) | `./mvnw verify` |
| Mutation testing (gate 80, report in `service/app/target/pit-reports`) | `./mvnw -pl service/app -am test-compile pitest:mutationCoverage` |
| Regenerate the typed BPMN process API (after editing a `.bpmn`) | `./mvnw -pl service/app generate-sources` |
| Regenerate + verify the OpenAPI contract | `./mvnw -pl service/app -am test -Dtest=OpenApiSpecExportTest -Dsurefire.failIfNoSpecifiedTests=false` then `git diff --exit-code openapi/openapi.json` |
| API scenarios (running stack) | `cd bruno && npx --yes @usebruno/cli@4.0.0 run . --env local -r` |
| BPMN lint | `npm ci && npm run lint:bpmn` |
| Backend OCI image | `./mvnw -pl service/app -am -DskipTests package spring-boot:build-image-no-fork` — [ADR-0011](docs/adr/0011-build-and-deployment-approach.md), CONTRIBUTING "Run it in containers" |

Maven gotchas (see [ADR-0013](docs/adr/0013-java-and-maven-instead-of-kotlin-and-gradle.md)):

- Run from the repo root and target the app with **`-pl service/app -am`** plus a lifecycle phase
  (`test-compile`, `test`, …) — `-am` builds `service/common-architecture-tests` in the same reactor.
  Without it (or with a goal-only call such as plain `pitest:mutationCoverage`), Maven resolves the arch
  suite from `~/.m2` — and sibling Miragon blueprints publish the same coordinates
  (`io.miragon.blueprint:app` / `common-architecture-tests:1.0-SNAPSHOT`), so that may be a foreign rule
  set. For the same reason don't `./mvnw install`: it would overwrite theirs; nothing here needs `~/.m2`.
- `-Dtest=…` together with `-am` needs **`-Dsurefire.failIfNoSpecifiedTests=false`**; with it, a pattern
  that matches nothing passes silently — always read the `Tests run:` line.
- Select tests **by class name** (`-Dtest=ArchitectureTest`, `-Dtest='*FooTest'`,
  `-Dtest='FooTest#method_name*'`). A package glob such as `io.miragon.blueprint.architecture.*` runs
  **0** architecture tests: their `@Nested` rule classes are inherited from
  `io.miragon.common.architecture.ServiceArchitectureTest` and reported under that package.

## Architecture — the rules are machine-enforced

The hexagonal rules live in `service/common-architecture-tests` — ArchUnit for the bytecode rules,
JavaParser for the source rules ([ADR-0014](docs/adr/0014-two-architecture-test-tools-archunit-and-javaparser.md))
— and **fail the build**. Read `HexagonalArchitectureTest.java` and
`NamingConventionArchitectureTest.java` before writing code; `service/app`'s `ArchitectureTest` wires
the whole suite in. The hard rules:

- **One inbound port per controller.** `onlyFulfilOneUseCase` counts constructor params in
  `application.port.inbound` and fails at >1. An inbox listing + a completion are two controllers.
- **No new top-level `config` package.** The containment rule ignores only *direct* members of the
  root package, so `io.miragon.blueprint.config` would fail. Cross-cutting `@Configuration` (CORS,
  OpenAPI, error handling) goes in `adapter.inbound.rest` — the `Configuration` suffix is whitelisted
  there.
- **`adapter/process` is generated.** Never hand-edit `*ProcessApi.java`; edit the `.bpmn` and re-run
  `./mvnw -pl service/app generate-sources` (the `bpmn-to-code-maven` plugin also regenerates it on
  every build).
- **Source rules (JavaParser, `JavaSourceGuidelinesTest`).** No wildcard imports except `java.util` in
  every Java file of the repo — main and test, all modules, static imports included
  (`import static org.mockito.Mockito.*` fails the build) — and at most one top-level type per file under
  the service's root package (`io.miragon.blueprint`). The generated `adapter.process` package is exempt
  from both.
- **Service tasks are external topics.** A custom model rule (`ServiceTaskExternalTopicRule`) requires
  every service task to be an external task with a topic — no embedded delegates. The trade-offs
  behind this are written up in [`docs/execution-and-task-listeners.md`](docs/execution-and-task-listeners.md).
- **Suffixes:** inbound port `UseCase|Query`; outbound `Port|Repository|Process`; service
  `Service|Configuration`; `adapter.inbound.rest` `Controller|Dto|Input|Mapper|Configuration`;
  `adapter.outbound` `PersistenceAdapter|Adapter|Mapper|Entity|Repository`.
- **Spring Data types stop at the adapter.** Ports own their own `Filter`/`Page`/`Criteria` types.

## Java Conventions

- **Records** for values: domain value objects and aggregates, port `Command`/`Result`/`Filter`/`Page`
  types (nested in their port), REST DTOs/inputs (nested in their controller). JPA entities stay classes.
- **Absence:** a port or service method that may find nothing returns `Optional<T>`; a nullable record
  component or parameter stays a plain reference and says so in its Javadoc.
- **Exception types are API:** `GlobalExceptionConfiguration` maps them to HTTP statuses — invalid input
  → `IllegalArgumentException` (400), unknown id → `IllegalStateException` (404).
- **Spring beans:** constructor injection into `private final` fields, one public constructor, no
  `@Autowired`; keep beans non-final. Logging via SLF4J (`LoggerFactory.getLogger(X.class)`). No Lombok.

## BPMN Quality Gates

- `bpmn-to-code` (Maven plugin, `generate-sources` phase) generates typed process constants from the
  models at build time; a custom model test requires every service task to be an external task with a
  topic.
- `bpmnlint` tooling lives at the **repo root** (`package.json`, `.bpmnlintrc`): `npm ci && npm run
  lint:bpmn`. It also runs on staged `.bpmn` via `.githooks/pre-commit` (install: `npm run hooks:install`).

## Testing

TDD. Match the test style to the layer:

| Layer | Test style |
|---|---|
| domain | plain unit tests |
| application service | Mockito unit tests (mock the ports) |
| `adapter.inbound.rest` | `@WebMvcTest` + `@MockitoBean` |
| `adapter.outbound.db` | `@DataJpaTest` |
| process end-to-end | CIB seven process tests (the real `@ProcessEngineWorker` beans consume the external service tasks) |

JUnit 5 + AssertJ + Mockito. Test methods are snake_case sentences
(`user_submits_a_leasing_request`), rendered with spaces by the `ReplaceUnderscores` display-name
generator; keep the given/when/then comments. Build test data with
`TestObjectBuilder.testLeasingApplication()…build()`.

**Mutation testing gates PRs at 80** (`./mvnw -pl service/app -am test-compile pitest:mutationCoverage`):
a test that executes without asserting will fail CI — except for code PIT does not mutate, notably record
compact constructors (the value-object validations; see ADR-0013), which only exact assertions in the
domain tests guard. Coverage says a line ran; mutation says a test
would have noticed. The PR gate runs **diff-scoped** (only the classes the PR changed, still blocking);
the **full-module** gate-80 sweep runs nightly. See ADR-0004.

## Verify After Each Task (targeted, not a full build)

- Backend service/controller: `./mvnw -pl service/app -am test -Dtest='*<Name>Test' -Dsurefire.failIfNoSpecifiedTests=false`
- Architecture only: `./mvnw -pl service/app -am test -Dtest=ArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false`
- Contract changed: regenerate the spec, then `git diff --exit-code openapi/openapi.json`

## Working with GitHub

Use the `gh` CLI. Write everything (issues, PRs, commit messages) in **English**. Use
**Conventional Commits** (`feat:`, `fix:`, `test:`, `chore:`, `docs:`, `ci:`, `build:`).

## ADRs

Architecture decisions are recorded in `docs/adr/` (0001–0014). Read them to understand *why* the
repo is shaped this way before proposing structural changes.

## Personality

You are a knowledgeable colleague, not someone who passively takes orders. If something proposed
doesn't look right, suggest corrections, ask critical questions, and push back where needed.
Challenge ideas that could benefit from further improvement or iterative refinement rather than just
accepting them at face value.
