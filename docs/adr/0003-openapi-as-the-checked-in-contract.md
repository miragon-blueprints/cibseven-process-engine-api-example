# 0003 — OpenAPI as the checked-in contract

- **Status:** Accepted
- **Date:** 2026-08-18

## Context

The Kotlin backend exposes a REST boundary that any consumer — another service, a generated client, a
CLI, or an integration test — has to program against. That boundary can be described three ways: (a)
hand-write a spec and hope the code matches it; (b) let each consumer hand-write its own types and hope
they match the backend; (c) derive the contract from the code. Options (a) and (b) drift silently — the
first symptom is a runtime 400 at the caller. We want the contract to be *impossible* to desync, and we
want a new contributor to be able to read it without running anything.

## Decision

The **backend is the single source of truth**, and the contract is **generated but committed**:

1. **springdoc** serves a live OpenAPI document from the annotated controllers
   (`@Operation(operationId = …)` gives each endpoint a stable, client-friendly method name).
2. `OpenApiSpecExportTest` — a code generator wearing a JUnit costume — fetches `/v3/api-docs`,
   re-serialises it **deterministically** (keys sorted, fixed two-space LF indenter, trailing newline,
   `servers` block dropped so the random test port can't cause churn) and writes
   **`openapi/openapi.json`** at the repo root. It runs inside `./gradlew build`.
3. CI regenerates the spec and runs **`git diff --exit-code`** on `openapi/openapi.json` — a **drift
   gate**. If a controller changed and the committed spec wasn't updated, the build fails.

Because the contract is a committed artifact, **any** consumer — an external client generator, another
service, or a test suite — can code against `openapi/openapi.json` without booting the backend, and
every API change shows up as a reviewable diff in the PR that makes it.

## Why generated-but-committed beats the alternatives

- **vs. hand-written spec/types:** eliminates the drift class entirely — the gate fails the PR, not the
  caller.
- **vs. generated-at-build (not committed):** the committed JSON is reviewable in every PR diff (an API
  change is *visible*), consumers can build without booting the backend, and offline/agent workflows
  keep working. The cost — a checked-in generated file — is paid down by the drift gate that keeps it
  honest.

## Consequences

- **Positive:** one contract, reviewable and gated; API changes are visible in review; consumers are
  decoupled from a running backend.
- **Negative / trade-offs:** a generated file lives in git; forgetting to regenerate is an *expected*
  failure mode — the gate is what makes that safe, so it must never be disabled.
- **Neutral:** determinism is a hard requirement of the export test — any non-deterministic serialisation
  would make the gate flap.

## Implementation notes

Two things bite when this runs on the embedded CIB seven engine:

- **Swagger UI vs. the CIB seven webclient.** The webapp registers its own resource handlers *and* its
  own `OpenAPI` bean. `OpenApiConfiguration`'s bean is marked `@Primary` so springdoc serves ours for
  `/api/**`, and `/camunda`, `/swagger-ui.html` and `/v3/api-docs` coexist (verified at build time). If
  a future upgrade breaks that, swap to `springdoc-openapi-starter-webmvc-api` (spec only, no UI) — a
  consumer only needs `/v3/api-docs`.
- **Jackson 3 date-time.** Spring Boot 4 ships Jackson 3, which defaults `WRITE_DATES_AS_TIMESTAMPS` on,
  and the webclient serves `/api` with its own mapper that ignores global config. springdoc types the
  fields as `string/date-time`, so DTO date fields are pinned with `@JsonFormat(shape = STRING)` to keep
  payload and contract in sync. Operation ids are set explicitly (`@Operation(operationId = …)`) so the
  generated method names stay clean and stable (e.g. `listLeasingApplications`).
