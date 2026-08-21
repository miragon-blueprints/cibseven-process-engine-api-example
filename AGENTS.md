# AGENTS.md

Guidance for AI agents (and humans) working in this repo. This is the real file; `CLAUDE.md` just
imports it.

## Project Overview

A headless **MiraVelo bike-leasing** example: one complete, runnable BPMN process automated with a
CIB seven embedded engine, driven through the **process-engine-api** (bpm-crafters) abstraction, with
an architecture enforced by tests.

- **Backend** (`service/app`) — Kotlin / Spring Boot 4, hexagonal, CIB seven 2.2.0 embedded engine.
  Package root `io.miragon.blueprint`.
- **Process-engine-api, not embedded delegates** — BPMN service tasks are `camunda:type="external"`
  topics consumed by `@ProcessEngineWorker` beans in `adapter/inbound/cibseven`; the engine is driven
  (start / correlate / complete) through `dev.bpmcrafters.processengineapi` in `adapter/outbound/cibseven`.
  There are **no** `JavaDelegate`/`ExecutionListener`/`TaskListener` and no `camunda:delegateExpression`.
- **The API contract** is `openapi/openapi.json`: springdoc generates it from the controllers, it is
  **committed and drift-gated** in CI so any REST change is caught. See ADR-0003.

## Development Setup

Two commands to a running backend:

```bash
docker compose -f stack/docker-compose.yml up -d   # Postgres
./gradlew :service:app:bootRun                      # backend + embedded engine on :8080
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
| Backend (arch + unit + process + model validation + spec export) | `./gradlew build` |
| Mutation testing (gate 80) | `./gradlew :service:app:pitest` |
| Regenerate the typed BPMN process API (after editing a `.bpmn`) | `./gradlew generateBpmnModels` |
| Regenerate + verify the OpenAPI contract | `./gradlew :service:app:test --tests "io.miragon.blueprint.openapi.OpenApiSpecExportTest"` then `git diff --exit-code openapi/openapi.json` |
| API scenarios (running stack) | `cd bruno && npx --yes @usebruno/cli run . --env local -r` |
| BPMN lint | `npm ci && npm run lint:bpmn` |
| Backend OCI image | `./gradlew :service:app:bootBuildImage` — [ADR-0011](docs/adr/0011-build-and-deployment-approach.md), CONTRIBUTING "Run it in containers" |

## Architecture — the rules are machine-enforced

The hexagonal rules live in `service/common-architecture-tests` (ArchUnit + Konsist) and **fail the
build**. Read `HexagonalArchitectureTest.kt` and `NamingConventionArchitectureTest.kt` before writing
code. The hard rules:

- **One inbound port per controller.** `onlyFulfilOneUseCase` counts constructor params in
  `application.port.inbound` and fails at >1. An inbox listing + a completion are two controllers.
- **No new top-level `config` package.** The containment rule ignores only *direct* members of the
  root package, so `io.miragon.blueprint.config` would fail. Cross-cutting `@Configuration` (CORS,
  OpenAPI, error handling) goes in `adapter.inbound.rest` — the `Configuration` suffix is whitelisted
  there.
- **`adapter/process` is generated.** Never hand-edit `*ProcessApi.kt`; edit the `.bpmn` and re-run
  `generateBpmnModels`.
- **Service tasks are external topics.** A custom model rule (`ServiceTaskExternalTopicRule`) requires
  every service task to be an external task with a topic — no embedded delegates. The trade-offs
  behind this are written up in [`docs/execution-and-task-listeners.md`](docs/execution-and-task-listeners.md).
- **Suffixes:** inbound port `UseCase|Query`; outbound `Port|Repository|Process`; service
  `Service|Configuration`; `adapter.inbound.rest` `Controller|Dto|Input|Mapper|Configuration`;
  `adapter.outbound` `PersistenceAdapter|Adapter|Mapper|Entity|Repository`.
- **Spring Data types stop at the adapter.** Ports own their own `Filter`/`Page`/`Criteria` types.

## BPMN Quality Gates

- `bpmn-to-code` generates typed process constants from the models at build time; a custom model
  test requires every service task to be an external task with a topic.
- `bpmnlint` tooling lives at the **repo root** (`package.json`, `.bpmnlintrc`): `npm ci && npm run
  lint:bpmn`. It also runs on staged `.bpmn` via `.githooks/pre-commit` (install: `npm run hooks:install`).

## Testing

TDD. Match the test style to the layer:

| Layer | Test style |
|---|---|
| domain | plain unit tests |
| application service | mockk unit tests (mock the ports) |
| `adapter.inbound.rest` | `@WebMvcTest` + MockkBean |
| `adapter.outbound.db` | `@DataJpaTest` |
| process end-to-end | CIB seven process tests (the real `@ProcessEngineWorker` beans consume the external service tasks) |

**Mutation testing gates PRs at 80** (`:service:app:pitest`): a test that executes without asserting
will fail CI. Coverage says a line ran; mutation says a test would have noticed. The PR gate runs
**diff-scoped** (only the classes the PR changed, still blocking); the **full-module** gate-80 sweep
runs nightly. See ADR-0004.

## Verify After Each Task (targeted, not a full build)

- Backend service/controller: `./gradlew :service:app:test --tests "*<Name>Test"`
- Architecture only: `./gradlew :service:app:test --tests "io.miragon.blueprint.architecture.*"`
- Contract changed: regenerate the spec, then `git diff --exit-code openapi/openapi.json`

## Working with GitHub

Use the `gh` CLI. Write everything (issues, PRs, commit messages) in **English**. Use
**Conventional Commits** (`feat:`, `fix:`, `test:`, `chore:`, `docs:`, `ci:`, `build:`).

## ADRs

Architecture decisions are recorded in `docs/adr/` (0001–0011). Read them to understand *why* the
repo is shaped this way before proposing structural changes.

## Personality

You are a knowledgeable colleague, not someone who passively takes orders. If something proposed
doesn't look right, suggest corrections, ask critical questions, and push back where needed.
Challenge ideas that could benefit from further improvement or iterative refinement rather than just
accepting them at face value.
