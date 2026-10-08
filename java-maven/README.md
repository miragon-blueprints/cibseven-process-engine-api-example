# Java + Maven variant

The bike-leasing service in **Java 21**, built with **Maven**, on Spring Boot 4 and an embedded
CIB seven 2.2 engine driven through the process-engine-api. It is the stack most enterprise teams and
our trainings use, and needs no Kotlin or Gradle knowledge.

<!-- variant:blueprint -->
> [!NOTE]
> Functionally identical to the [Kotlin + Gradle variant](../kotlin-gradle/README.md), which is the one
> we recommend when you are free to choose. Same process, same REST contract, same scenarios.
<!-- /variant:blueprint -->

## 🧰 Commands

Run them from this directory; the Maven wrapper is included. Postgres comes from
`docker compose -f ../stack/docker-compose.yml up -d`.

| Task | Command |
|---|---|
| Run the service on :8080 | `./mvnw -pl service/app -am spring-boot:run` |
| Full build (arch + Checkstyle + unit + process + model validation + spec export) | `./mvnw verify` |
| Mutation testing (gate 80) | `./mvnw -pl service/app -am test-compile pitest:mutationCoverage` |
| Regenerate the typed process API after editing a `.bpmn` | `./mvnw -pl service/app generate-sources` |
| Build the OCI image | `./mvnw -pl service/app -am -DskipTests package spring-boot:build-image-no-fork` |

## 📂 Layout

```
pom.xml                        parent: all versions and plugin management
config/checkstyle/             the two source rules (no wildcard imports, one top-level type per file)
service/
  common-architecture-tests/   reusable ArchUnit rule suite (src/main)
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

- **Hexagonal architecture.** Domain and use cases never depend on CIB seven. `common-architecture-tests`
  enforces layering, dependency direction and naming with **ArchUnit**; **Checkstyle** adds the two
  source rules. A service opts in with `class ArchitectureTest extends ServiceArchitectureTest`.
- **process-engine-api, not embedded delegates.** Every service task is an external task with a topic,
  consumed by a `@ProcessEngineWorker` bean; starting, correlating and completing go through
  `dev.bpmcrafters.processengineapi`. The trade-offs are written up in
  [`../docs/execution-and-task-listeners.md`](../docs/execution-and-task-listeners.md).
- **Generated process API.** The [`bpmn-to-code`](https://github.com/Miragon/bpmn-to-code) Maven plugin
  turns each `.bpmn` into a typed, node-centric `*ProcessApi` class on every build. Never hand-edit
  `adapter/process`.
- **Unit tests** (JUnit 5 + Mockito) cover every domain type, service and adapter — controllers via
  `@WebMvcTest` with `@MockitoBean`, persistence via `@DataJpaTest`. The workers are covered by the
  process tests.
- **Process tests** (`cibseven-bpm-assert`) drive the deployed model while the real
  `@ProcessEngineWorker` beans consume the external service tasks, and assert the walked path as a
  compile-checked `PathWalk`.
- **Model validation** (`bpmn-to-code-testing`) checks the models at build time, including a custom rule
  that every service task is an external task with a topic.
- **Mutation testing** (PIT, gate 80) — diff-scoped on pull requests
  (`-DmutationTargetClasses="a.b.*"`), full sweep nightly.
- **OpenAPI contract.** A test exports the springdoc spec to [`../openapi/openapi.json`](../openapi/openapi.json);
  CI fails on drift.
- **Database and observability.** Flyway owns the schema and Hibernate only validates; Spring Boot
  Actuator exposes health probes and Prometheus metrics under `/actuator`.

<!-- variant:blueprint -->
## 🔀 What differs from the Kotlin variant

Only idioms: **records** and `Optional` instead of `data` classes and nullable types, **Mockito** instead
of MockK, **SLF4J** instead of kotlin-logging, **Checkstyle** instead of Konsist. Records carry no
nullability, so the REST DTOs declare it with annotations to produce the same contract. The libraries
around the engine (process-engine-api, bpmn-to-code) are written in Kotlin, so the Kotlin standard
library is on the runtime classpath here too; the parent `pom.xml` keeps it on the same version.
<!-- /variant:blueprint -->
