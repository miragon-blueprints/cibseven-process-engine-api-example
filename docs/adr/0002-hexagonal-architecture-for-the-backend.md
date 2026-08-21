# 0002 — Hexagonal architecture for the backend

- **Status:** Accepted
- **Date:** 2026-08-18

## Context

The backend automates a real BPMN process with many collaborators: a process engine, JPA persistence,
a simulated dealer, notification and contract stubs. If controllers, workers, and the engine client
call each other freely, business logic leaks into infrastructure and the process becomes untestable in
isolation. A blueprint has to demonstrate a structure that stays sane as this fan-out grows — and it
has to make that structure *checkable*, not merely documented, so a forker (or an AI agent) cannot
quietly erode it.

## Decision

We structure `service/app` as a **hexagon (ports & adapters)** under `io.miragon.blueprint`:

- `domain/` — pure Kotlin value objects and aggregates; no framework imports.
- `application/port/inbound` — one **`*UseCase`** (state-changing) or **`*Query`** (read) interface per
  operation. `application/port/outbound` — **`*Repository` / `*Port` / `*Process`** interfaces.
- `application/service` — one `*Service` implementing exactly one inbound port; it may not call another
  service or any inbound port.
- `adapter/inbound/{rest,cibseven}` — driving adapters (REST controllers and the external-topic
  `@ProcessEngineWorker` beans that consume the engine's service-task work).
- `adapter/outbound/{db,cibseven,dealer,notification,contract,insurance}` — driven adapters.
- `adapter/process` — the **generated** `*ProcessApi` (bpmn-to-code) plus engine config; a technical
  seam that fits neither side of the split.

These rules are **enforced by the reusable ArchUnit + Konsist suite** in
`service/common-architecture-tests`, wired into every module's tests so `./gradlew build` fails on a
violation:

- `HexagonalArchitectureTest` — the layered-dependency graph (domain depends on nothing; ports are
  interfaces; an in-adapter offers exactly one use-case; out-adapters never touch inbound ports).
- `NamingConventionArchitectureTest` — the suffix rules per package (`Controller`, `Worker`,
  `PersistenceAdapter`, `UseCase`, `Query`, …).
- There is deliberately **no `config` package**; Spring configuration lives beside the adapter it
  configures (`OpenApiConfiguration`, `GlobalExceptionConfiguration`, `HistoryCleanupConfiguration`).
- The generated `adapter/process` package is **explicitly excluded** from both suites — it is machine-
  written and does not follow the hand-written conventions.

Service-task logic lives **only** in external-topic `@ProcessEngineWorker` beans under
`adapter/inbound/cibseven` — never in an embedded `JavaDelegate`, `ExecutionListener`, or `TaskListener`
(see [execution-and-task-listeners.md](../execution-and-task-listeners.md) for that trade-off).

## Consequences

- **Positive:** business logic is engine- and framework-agnostic and unit-testable behind ports; the
  structure is a *fitness function*, not a style guide — agents get instant, local feedback.
- **Negative / trade-offs:** more indirection (a port + a service + an adapter per operation) than a
  layered CRUD app; the "one use-case per adapter" rule means many small classes.
- **Neutral:** the generated-code seam (`adapter/process`) is a permanent, documented exception; see
  [ADR-0003](0003-openapi-as-the-checked-in-contract.md) for the analogous generated seam on the
  API-contract side.
