# Kotlin + Gradle variant

The bike-leasing service in **Kotlin 2.4**, built with **Gradle** and a `libs.versions.toml` version
catalog, on Spring Boot 4 and an embedded CIB seven 2.2 engine driven through the process-engine-api.

<!-- variant:blueprint -->
> [!TIP]
> **This is the stack we recommend** when you are free to choose. Null-safety keeps the domain model and
> the OpenAPI contract precise without annotations, `data` and `value` classes keep the domain compact,
> and Konsist adds source-level architecture rules that bytecode analysis cannot express. The
> [Java + Maven variant](../java-maven/README.md) is functionally identical for teams bound to that stack.
<!-- /variant:blueprint -->

## 🧰 Commands

Run them from this directory. Postgres comes from `docker compose -f ../stack/docker-compose.yml up -d`.

| Task | Command |
|---|---|
| Run the service on :8080 | `./gradlew :service:app:bootRun` |
| Full build (arch + unit + process + model validation + spec export) | `./gradlew build` |
| Mutation testing (gate 80) | `./gradlew :service:app:pitest` |
| Regenerate the typed process API after editing a `.bpmn` | `./gradlew generateBpmnModels` |
| Build the OCI image | `./gradlew :service:app:bootBuildImage` |

## 📂 Layout

```
service/
  common-architecture-tests/   reusable ArchUnit + Konsist rule suite (src/main)
  app/                         the service, package root io.miragon.blueprint
    adapter/inbound/rest        REST controllers + OpenAPI / problem-details config
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

<!-- variant:blueprint -->
The resources are kept identical to the other variant's; CI fails when they differ.
<!-- /variant:blueprint -->

## 🧱 How it is built

- **Hexagonal architecture.** Domain and use cases never depend on CIB seven, so the business logic is
  testable and the engine replaceable. `common-architecture-tests` enforces it with **ArchUnit**
  (bytecode: layering, dependency direction, naming) and **Konsist** (source: one declaration per file,
  no wildcard imports); a service opts in with `class ArchitectureTest : ServiceArchitectureTest(...)`.
- **process-engine-api, not embedded delegates.** Every service task is an external task with a topic,
  consumed by a `@ProcessEngineWorker` bean; starting, correlating and completing go through
  `dev.bpmcrafters.processengineapi`. The trade-offs are written up in
  [`../docs/execution-and-task-listeners.md`](../docs/execution-and-task-listeners.md).
- **Generated process API.** The [`bpmn-to-code`](https://github.com/Miragon/bpmn-to-code) Gradle plugin
  turns each `.bpmn` into a typed, node-centric `*ProcessApi` object, so element ids, messages, timers,
  variables and the paths the process tests walk are compile-checked. Never hand-edit `adapter/process`.
- **Unit tests** (JUnit 5 + MockK) cover every domain type, service and adapter — controllers via
  `@WebMvcTest`, persistence via `@DataJpaTest`. The workers are covered by the process tests.
- **Process tests** (`cibseven-bpm-assert`) drive the deployed model: timers and async continuations are
  fired and messages correlated by hand, while the real `@ProcessEngineWorker` beans consume the external
  service tasks. The walked path is asserted as a compile-checked `ProcessPath`.
- **Model validation** (`bpmn-to-code-testing`) checks the models at build time, including a custom rule
  that every service task is an external task with a topic.
- **Mutation testing** (PIT, gate 80) grades assertion strength — diff-scoped on pull requests, full
  sweep nightly.
- **OpenAPI contract.** A test exports the springdoc spec to [`../openapi/openapi.json`](../openapi/openapi.json);
  CI fails on drift.
- **Database and observability.** Flyway owns the schema and Hibernate only validates; Spring Boot
  Actuator exposes health probes and Prometheus metrics under `/actuator`.
