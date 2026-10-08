# AGENTS.md

Guidance for AI agents (and humans) working in this repo. This is the real file; `CLAUDE.md` just
imports it (see [ADR-0005](docs/adr/0005-agents-md-as-the-single-source.md)).

## Project Overview

A headless **MiraVelo bike-leasing** example: one complete, runnable BPMN process automated with a
CIB seven embedded engine, driven through the **process-engine-api** (bpm-crafters) abstraction, with
an architecture enforced by tests.

- **Backend** — Spring Boot 4, hexagonal, CIB seven 2.2.0 embedded engine. Package root
  `io.miragon.blueprint`.
- **Process-engine-api, not embedded delegates** — BPMN service tasks are `camunda:type="external"`
  topics consumed by `@ProcessEngineWorker` beans in `adapter/inbound/cibseven`; the engine is driven
  (start / correlate / complete) through `dev.bpmcrafters.processengineapi` in `adapter/outbound/cibseven`.
  There are **no** `JavaDelegate`/`ExecutionListener`/`TaskListener` and no `camunda:delegateExpression`.
  <!-- variant:blueprint -->
- **Two equivalent variants on `main`** — `kotlin-gradle/` (Kotlin, **the recommended stack**) and
  `java-maven/` (Java 21, for teams bound to it and for trainings). Each is self-contained and
  carries its own copy of the process models, forms, migrations and `application.yaml`; the
  `Blueprint Checks` workflow fails when the two `src/main/resources` trees differ, so **edit a model in
  one variant and copy it to the other**. The variants must stay functionally identical: **make every
  change in behaviour in both variants in the same PR.** Changes that only concern one language's
  idioms stay on that side. See [ADR-0013](docs/adr/0013-two-stack-variants-side-by-side-on-main.md).
  The OpenAPI contract below is drift-gated against **both** variants.
  Stack-specific content in shared files (docs, Dependabot, Conductor settings) is wrapped in
  `variant:<name>` markers so `scripts/create-starter.sh` can strip it — see
  [docs/starter.md](docs/starter.md).
  <!-- /variant:blueprint -->
- **The API contract** is `openapi/openapi.json`: springdoc generates it from the controllers, it is
  **committed and drift-gated** in CI so any REST change is caught. See
  [ADR-0003](docs/adr/0003-openapi-as-the-checked-in-contract.md).

## Repository Map

<!-- variant:kotlin-gradle variant:nested -->
- `kotlin-gradle/` — the service in Kotlin, built with Gradle
  <!-- /variant:kotlin-gradle -->
  <!-- variant:java-maven variant:nested -->
- `java-maven/` — the service in Java 21, built with Maven
  <!-- /variant:java-maven -->

The service:

```
service/
  common-architecture-tests/   reusable architecture rule suite
  app/                         the CIB seven bike-leasing service (hexagonal)
    adapter/inbound/rest        domain REST controllers + OpenAPI / problem-details config
    adapter/inbound/cibseven    process-engine-api workers for the BPMN service tasks
    adapter/outbound/cibseven   drives the engine through the process-engine-api + task inbox
    adapter/outbound/db         JPA persistence (leasing applications + bike portfolio)
    adapter/outbound/…          simulated dealer / contract / insurance / notification adapters
    adapter/process             generated *ProcessApi (bpmn-to-code) + engine config
    application/{port,service}  use-case ports and their services
    domain/{leasing,bike}       pure domain model
    resources/{bpmn,dmn,forms}  the process models and Camunda Forms
    resources/db/migration      Flyway forward-only migrations
```

Around it:

```
bruno/                         REST scenarios (happy-path / escalation / abort / not-solvent / …)
openapi/                       the checked-in, drift-gated OpenAPI contract (openapi.json)
docs/                          Architecture Decision Records + diagrams
stack/                         Postgres dev stack (docker compose)
.github/                       pre-merge + nightly pipelines + Dependabot
.githooks/                     pre-commit hook (bpmnlint on staged .bpmn)
scripts/create-starter.sh      strips the repo down to one variant (docs/starter.md)
package.json / .bpmnlintrc     root-level bpmnlint config + git-hook installer (npm run lint:bpmn)
```

## Development Setup

Two commands to a running backend — Postgres, then the backend + embedded engine on :8080:

```bash
docker compose -f stack/docker-compose.yml up -d
```

<!-- variant:kotlin-gradle -->
```bash
cd kotlin-gradle && ./gradlew :service:app:bootRun
```
<!-- /variant:kotlin-gradle -->
<!-- variant:java-maven -->
```bash
cd java-maven && ./mvnw -pl service/app -am spring-boot:run
```
<!-- /variant:java-maven -->

### Ports (one source of truth — keep README, this file and `.conductor/settings.toml` in sync)

| What | Port |
|---|---|
| Postgres | 5432 |
| Backend (REST + engine-rest) | 8080 |
| CIB seven Cockpit / webapps | 8080/camunda (admin/admin) |
| OpenAPI spec · Swagger UI | 8080/v3/api-docs · 8080/swagger-ui.html |
| Actuator (health/liveness/readiness · prometheus) | 8080/actuator |

## Build Commands

<!-- variant:kotlin-gradle -->
| Area (run in `kotlin-gradle/`) | Command |
|---|---|
| Backend (arch + unit + process + model validation + spec export) | `./gradlew build` |
| Mutation testing (gate 80) | `./gradlew :service:app:pitest` |
| Regenerate the typed BPMN process API (after editing a `.bpmn`) | `./gradlew generateBpmnModels` |
| Regenerate the OpenAPI contract | `./gradlew :service:app:test --tests "io.miragon.blueprint.openapi.OpenApiSpecExportTest"` |
| Backend OCI image | `./gradlew :service:app:bootBuildImage` |
<!-- /variant:kotlin-gradle -->

<!-- variant:java-maven -->
| Area (run in `java-maven/`) | Command |
|---|---|
| Backend (arch + Checkstyle + unit + process + model validation + spec export) | `./mvnw verify` |
| Mutation testing (gate 80) | `./mvnw -pl service/app -am test-compile pitest:mutationCoverage` |
| Regenerate the typed BPMN process API (after editing a `.bpmn`) | `./mvnw -pl service/app generate-sources` |
| Regenerate the OpenAPI contract | `./mvnw -pl service/app -am test -Dtest=OpenApiSpecExportTest -Dsurefire.failIfNoSpecifiedTests=false` |
| Backend OCI image | `./mvnw -pl service/app -am -DskipTests package spring-boot:build-image-no-fork` |
<!-- /variant:java-maven -->

| Area (repo root) | Command |
|---|---|
| Verify the OpenAPI contract after regenerating it | `git diff --exit-code openapi/openapi.json` |
| API scenarios (running stack) | `cd bruno && npx --yes @usebruno/cli@4.0.0 run . --env local -r` |
| BPMN lint | `npm ci && npm run lint:bpmn` |

The OCI image decision is [ADR-0011](docs/adr/0011-build-and-deployment-approach.md); the how-to is
CONTRIBUTING "Run it in containers".

## Architecture — the rules are machine-enforced

The hexagonal rules live in `service/common-architecture-tests` (ArchUnit, plus a source-level tool for
the rules bytecode cannot express — see
[ADR-0007](docs/adr/0007-two-architecture-test-tools-archunit-and-konsist.md)) and **fail the build**.
Read `HexagonalArchitectureTest` and `NamingConventionArchitectureTest` before writing code. The hard
rules:

- **One inbound port per controller.** `onlyFulfilOneUseCase` counts constructor params in
  `application.port.inbound` and fails at >1. An inbox listing + a completion are two controllers.
- **No new top-level `config` package.** The containment rule ignores only *direct* members of the
  root package, so `io.miragon.blueprint.config` would fail. Cross-cutting `@Configuration` (CORS,
  OpenAPI, error handling) goes in `adapter.inbound.rest` — the `Configuration` suffix is whitelisted
  there.
- **`adapter/process` is generated.** Never hand-edit `*ProcessApi` or the shared
  `ServiceTasks`/`Messages`/`ProcessVariables`/`Errors`/`Escalations` files; edit the `.bpmn` and
  regenerate.
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
- Since bpmn-to-code 6 the API is node-centric: `<Process>ProcessApi.FlowNodes.<Node>` carries the
  element (`.id`, `ELEMENT_ID`), its `Variables` and its successors (`Next`). Process tests assert
  the walked path as a compile-checked path built from these nodes instead of hand-maintained
  element-id lists.
- `bpmnlint` tooling lives at the **repo root** (`package.json`, `.bpmnlintrc`): `npm ci && npm run
  lint:bpmn`. It also runs on staged `.bpmn` via `.githooks/pre-commit` (install: `npm run hooks:install`).

## Testing

TDD. Match the test style to the layer:

| Layer | Test style |
|---|---|
| domain | plain unit tests |
| application service | unit tests with mocked ports |
| `adapter.inbound.rest` | `@WebMvcTest` with the use case mocked |
| `adapter.outbound.db` | `@DataJpaTest` |
| process end-to-end | CIB seven process tests (the real `@ProcessEngineWorker` beans consume the external service tasks), paths asserted via the generated process API |

**Mutation testing gates PRs at 80**: a test that executes without asserting
will fail CI. Coverage says a line ran; mutation says a test would have noticed. The PR gate runs
**diff-scoped** (only the classes the PR changed, still blocking); the **full-module** gate-80 sweep
runs nightly. See [ADR-0004](docs/adr/0004-mutation-testing-as-a-blocking-pr-gate.md).

## Verify After Each Task (targeted, not a full build)

<!-- variant:blueprint -->
Verify the variant you touched — which, for a change in behaviour, is both.
<!-- /variant:blueprint -->

<!-- variant:kotlin-gradle -->
In `kotlin-gradle/`:

- Backend service/controller: `./gradlew :service:app:test --tests "*<Name>Test"`
- Architecture only: `./gradlew :service:app:test --tests "io.miragon.blueprint.architecture.*"`
  <!-- /variant:kotlin-gradle -->

<!-- variant:java-maven -->
In `java-maven/`:

- Backend service/controller: `./mvnw -pl service/app -am test -Dtest="<Name>Test" -Dsurefire.failIfNoSpecifiedTests=false`
- Architecture only: `./mvnw -pl service/app -am test -Dtest="ArchitectureTest" -Dsurefire.failIfNoSpecifiedTests=false`
  <!-- /variant:java-maven -->

From the repo root:

- Contract changed: regenerate the spec, then `git diff --exit-code openapi/openapi.json`
- Process changed: regenerate the process API, then the `process.*` tests

## Working with GitHub

Use the `gh` CLI. Write everything (issues, PRs, commit messages) in **English**. Use
**Conventional Commits** (`feat:`, `fix:`, `test:`, `chore:`, `docs:`, `ci:`, `build:`).

## ADRs

Architecture decisions are recorded in `docs/adr/`. Read them to understand *why* the repo is shaped
this way before proposing structural changes.

## Personality

You are a knowledgeable colleague, not someone who passively takes orders. If something proposed
doesn't look right, suggest corrections, ask critical questions, and push back where needed.
Challenge ideas that could benefit from further improvement or iterative refinement rather than just
accepting them at face value.
