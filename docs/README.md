# Documentation

Deeper context that the [README](../README.md) deliberately leaves out — the *why* behind the repo's
shape. This folder holds the **Architecture Decision Records** (the decisions) and the **diagrams**.

An ADR captures a **decision** — dated, numbered, and effectively immutable once accepted (a reversal
gets a new ADR, not an edit). Procedures are not decisions, so they live elsewhere: setup, the ports,
the dev loop and the smoke test in [CONTRIBUTING.md](../CONTRIBUTING.md); the day-to-day workflow in
[AGENTS.md](../AGENTS.md). Where a procedure has a decision behind it, that reasoning is in the
relevant ADR's own Decision section (e.g. the contract flow in ADR-0003, the mutation gate in ADR-0004).

## Architecture Decision Records

Each non-obvious decision is recorded as an ADR in [`adr/`](adr/), in a MADR/Nygard-lite format
(Status · Context · Decision · Consequences). They are numbered from 0001, never renumbered, and
copied from [`adr/0000-adr-template.md`](adr/0000-adr-template.md).

| ADR | Decision |
|---|---|
| [0001](adr/0001-record-architecture-decisions.md) | Record architecture decisions (one Markdown file per decision). |
| [0002](adr/0002-hexagonal-architecture-for-the-backend.md) | Hexagonal architecture for the backend, machine-enforced by ArchUnit + Konsist. |
| [0003](adr/0003-openapi-as-the-checked-in-contract.md) | OpenAPI as the checked-in, drift-gated contract for any API consumer. |
| [0004](adr/0004-mutation-testing-as-a-blocking-pr-gate.md) | Mutation testing as a blocking PR gate. |
| [0005](adr/0005-agents-md-as-the-single-source.md) | `AGENTS.md` as the single source of agent instructions. |
| [0006](adr/0006-fixed-ports-for-v1-portless-as-the-upgrade.md) | Fixed ports for v1, portless as the upgrade path. |
| [0007](adr/0007-two-architecture-test-tools-archunit-and-konsist.md) | Two architecture-test tools: ArchUnit (bytecode) and Konsist (source). |
| [0008](adr/0008-track-the-latest-major-versions.md) | Deliberately track the latest major versions across the stack. |
| [0009](adr/0009-actuator-probes-and-prometheus-metrics.md) | Actuator health/liveness/readiness probes and Prometheus metrics, exposed out of the box. |
| [0010](adr/0010-flyway-for-database-migrations.md) | Flyway for versioned schema migrations; Hibernate switches to `validate`. |
| [0011](adr/0011-build-and-deployment-approach.md) | Build & deployment: `bootBuildImage` OCI image for the headless backend. |

## Notes

- [`execution-and-task-listeners.md`](execution-and-task-listeners.md) — why this blueprint keeps all
  engine-facing work on the process-engine-api (external-task workers, no execution/task listeners), and
  the abstraction-vs-capability trade-off if you ever need native listeners.

## Diagrams

- [`assets/bike-leasing.png`](assets/bike-leasing.png) — the BPMN process at a glance.
