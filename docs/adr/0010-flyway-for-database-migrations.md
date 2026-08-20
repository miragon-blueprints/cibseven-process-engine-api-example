# 0010 — Flyway for database schema migrations

- **Status:** Accepted
- **Date:** 2026-08-20

## Context

The backend booted with `spring.jpa.hibernate.ddl-auto: create`, so Hibernate **dropped and
recreated the whole schema on every start**. Convenient for a throwaway demo, but this template aims
to be production-shaped ([ADR-0008](0008-track-the-latest-major-versions.md),
[ADR-0009](0009-actuator-probes-and-prometheus-metrics.md)), and a fork can't go to production that
way: no history of how the schema evolved, no reviewable diff for a column change, and every restart
destroys the data. Production needs schema changes that are **versioned, reviewable in a PR,
repeatable, and non-destructive**.

Two concerns share the Postgres database: our application tables (`bike_portfolio`,
`leasing_application`) and the CIB seven engine's `ACT_*` tables. The engine already manages its own
schema via `camunda.bpm.database.schema-update: true`, so a migration tool must own **only** the
application tables.

## Decision

We use **[Flyway](https://flywaydb.org/)** as the owner of the application schema and switch Hibernate
to **`ddl-auto: validate`** — Hibernate now only checks that the mapped entities match the migrated
schema and never creates or drops a table.

- **Dependencies** (`gradle/libs.versions.toml`, `database` bundle):
  **`spring-boot-starter-flyway`** + `flyway-database-postgresql`. The starter is required, not raw
  `flyway-core`: Spring Boot 4 modularised the Flyway autoconfiguration out of
  `spring-boot-autoconfigure`, so `flyway-core` alone puts Flyway on the classpath but never wires it
  to the datasource (migrations silently don't run). Postgres additionally needs the separate
  `flyway-database-postgresql` module. Versions come from the Spring Boot BOM.
- **Migrations** live in `service/app/src/main/resources/db/migration/`, named `V{n}__description.sql`,
  **forward-only** (an applied migration is checksummed and never edited — fix it with a new one).
  Flyway applies pending migrations on start-up, before Hibernate validates, and records each in its
  `flyway_schema_history` table. The baseline `V1__init.sql` reproduces the two current entities.
- **Seed vs. schema split:** Flyway owns **schema only**. The bike catalogue stays in
  `BikeCatalogueSeeder` (an idempotent `ApplicationRunner`), because that data — especially the
  `BIKE-OOS` out-of-stock entry — is a **demo/test fixture, not production reference data**, and must
  not be baked into a fork's real database.
- **Tests keep Flyway off.** The `test` profile runs on in-memory H2 with Hibernate building the
  schema (`ddl-auto: create-drop`); `spring.flyway.enabled: false` in `application-test.yaml` stops the
  Postgres-flavoured baseline from running against H2. The migrations are therefore exercised by the
  real-Postgres paths — local `bootRun` and the Bruno API-scenario CI job.

The how-to (adding a migration, the one-time reset of an old dev DB) is in
[CONTRIBUTING.md](../../CONTRIBUTING.md).

## Consequences

- **Positive:** schema changes are versioned SQL, reviewed in the PR that needs them, and applied
  repeatably to any environment; data survives restarts; a fork is production-shaped from day one.
- **Negative / trade-offs:** a schema change is now two coordinated edits (the entity **and** a new
  `V{n}__…sql`); a dev DB previously built by `ddl-auto: create` has no `flyway_schema_history`, so it
  must be dropped once when adopting Flyway (`docker compose … down -v`).
- **Neutral:** real reference data, if it ever appears, would move into a Flyway **repeatable**
  migration (`R__…sql`) rather than the seeder. Verifying migrations against real Postgres in the unit
  suite (a Testcontainers `@DataJpaTest`) is the documented upgrade path — deferred because no
  Testcontainers infrastructure exists yet.
