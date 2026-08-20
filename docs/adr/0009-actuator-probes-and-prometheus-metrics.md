# 0009 — Actuator probes and Prometheus metrics

- **Status:** Accepted
- **Date:** 2026-08-20

## Context

The backend shipped no operational endpoints: no health check for an orchestrator to probe, no
metrics for monitoring to scrape. A greenfield template that aims to be "production-shaped" should be
deploy- and ops-ready out of the box. Spring Boot makes this cheap — `spring-boot-starter-actuator`
plus a Micrometer registry — so the cost/benefit is clearly in favour. The open question was only
*how much* to expose, and whether the embedded CIB seven engine needs its own health contributor.

## Decision

We add **`spring-boot-starter-actuator` + `micrometer-registry-prometheus`** (via the
`defaultService` bundle in `gradle/libs.versions.toml`) and expose a **minimal, documented set** in
`application.yaml`: `health`, `info`, `metrics`, `prometheus`. Health **liveness/readiness groups**
are enabled (`management.endpoint.health.probes.enabled=true`), giving `/actuator/health/liveness`
and `/actuator/health/readiness` for orchestration. `springBoot { buildInfo() }` populates
`/actuator/info`.

We **do not** add a custom engine health contributor: the engine shares the Spring datasource, which
actuator's built-in `db` indicator already covers. We set **`show-details: never`** because this
blueprint ships no authentication and `/actuator` is therefore publicly reachable — a one-line
comment in `application.yaml` documents how to flip it to `always`. **OpenTelemetry tracing is
deferred** as a natural but non-required next step.

The actuator paths sit outside springdoc's `paths-to-match: /api/**`, so the checked-in
`openapi/openapi.json` contract (see [ADR-0003](0003-openapi-as-the-checked-in-contract.md)) is
unaffected.

## Consequences

- **Positive:** the app is orchestration- and observability-ready out of the box; probes and a
  Prometheus scrape endpoint exist with no bespoke code, and the built-in `db` indicator reflects the
  datasource the engine depends on.
- **Negative / trade-offs:** with no Spring Security in the repo, `/actuator` is open — acceptable
  for a local/blueprint stack but a follow-up (auth, or binding management to a separate port) before
  real production exposure; component-level health detail is hidden by default as a result.
- **Neutral:** engine-specific health and OpenTelemetry tracing remain available as later additions,
  tracked separately.
