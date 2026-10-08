# Contributing

Thanks for your interest in the CIB seven bike-leasing blueprint! Contributions of all kinds are
welcome — bug reports, feature ideas, docs, and code.

## Getting started

```bash
git clone git@github.com:miragon-blueprints/cibseven-process-engine-api-example.git
cd cibseven-process-engine-api-example
npm ci && npm run hooks:install                                # BPMN lint + git hooks
```

You need **JDK 21**, **Node ≥ 22.12** (for the BPMN lint tooling), and **Docker (or Podman)** for
Postgres.

<!-- variant:blueprint -->
The service exists in two equivalent variants: [`kotlin-gradle/`](kotlin-gradle/README.md) (recommended)
and [`java-maven/`](java-maven/README.md). Run either one.
<!-- /variant:blueprint -->

The build wrapper is included, so nothing else needs installing. Start Postgres, then the backend and
embedded engine on :8080:

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

### Ports

| What | Port |
|---|---|
| Postgres | 5432 |
| Backend (REST + `/engine-rest`) | 8080 |
| CIB seven Cockpit / webapps | 8080/camunda (admin/admin) |
| OpenAPI spec · Swagger UI | 8080/v3/api-docs · 8080/swagger-ui.html |
| Actuator (health · liveness/readiness · prometheus) | 8080/actuator |

Under Conductor the ports are fixed and the workspace runs `nonconcurrent` (see
[ADR-0006](docs/adr/0006-fixed-ports-for-v1-portless-as-the-upgrade.md)).

### Smoke test

Drive the running app with the Bruno API scenarios:

```bash
cd bruno && npx --yes @usebruno/cli@4.0.0 run . --env local -r
```

They start a case over the domain REST endpoints, complete user tasks and fire timer jobs via the
CIB seven `/engine-rest` API, and assert the whole flow — happy path, escalation, abort, DMN
rejection, the bike-unavailable → alternative-selection loop, and the incident demo.

Confirm <http://localhost:8080/camunda> (admin/admin), <http://localhost:8080/swagger-ui.html> and
<http://localhost:8080/actuator/health> (status `UP`) all load.

## Run it in containers

The dev loop above runs the backend from source. To run it as an OCI image — **app + Postgres** —
build the image with Spring's buildpacks (no Dockerfile), then run it against Postgres. The rationale
is in [ADR-0011](docs/adr/0011-build-and-deployment-approach.md).

Build the OCI image; it produces `cibseven-process-engine-api-example/app:1.0-SNAPSHOT`:

<!-- variant:kotlin-gradle -->
```bash
(cd kotlin-gradle && ./gradlew :service:app:bootBuildImage)
```
<!-- /variant:kotlin-gradle -->
<!-- variant:java-maven -->
```bash
(cd java-maven && ./mvnw -pl service/app -am -DskipTests package spring-boot:build-image-no-fork)
```
<!-- /variant:java-maven -->

Then bring up Postgres:

```bash
docker compose -f stack/docker-compose.yml up -d
```

**Podman:** the image build needs a Docker-API socket. Expose podman's, then build the image as above:

```bash
podman system service --time=0 unix:///tmp/podman.sock &
export DOCKER_HOST=unix:///tmp/podman.sock
```

**Configuration.** `application.yaml` ships dev defaults; the deploy-relevant values are read from the
environment (they win over the baked defaults):

| Env var | Purpose | Default |
|---|---|---|
| `SPRING_DATASOURCE_URL` | JDBC URL | `jdbc:postgresql://postgres:5432/bikeleasing` |
| `SPRING_DATASOURCE_USERNAME` / `_PASSWORD` | DB credentials | `admin` / `admin` |

> **Not production-hardened.** The image carries the example `jwtSecret` and admin/admin credentials
> from `application.yaml`. Override them (and the DB credentials) before running anywhere real. Schema
> is owned by Flyway and Hibernate only validates ([ADR-0010](docs/adr/0010-flyway-for-database-migrations.md)),
> so the Postgres volume persists across `down`/`up` — reset it with `docker compose -f
> stack/docker-compose.yml down -v`.

## Scripts

The build, mutation-testing and code-generation commands are listed in the README next to the code:

<!-- variant:kotlin-gradle -->
- [`kotlin-gradle/README.md`](kotlin-gradle/README.md#-commands)
  <!-- /variant:kotlin-gradle -->
  <!-- variant:java-maven -->
- [`java-maven/README.md`](java-maven/README.md#-commands)
  <!-- /variant:java-maven -->

From the repo root:

```bash
npm ci && npm run lint:bpmn             # bpmnlint the .bpmn models
```

## Ground rules

- **Start from an issue.** Every change traces back to one — open an issue (or pick an existing one)
  and agree on the approach *before* you write code, then reference it in the PR (`Closes #123`).
  This keeps substantial changes discussed up front and the history navigable.
- **Read [`AGENTS.md`](AGENTS.md) first.** It is the single source of guidance for humans and AI
  agents alike.
- **Conventional Commits.** Commit messages and PR titles follow
  [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `docs:`,
  `refactor:`, `test:`, `chore:`). Write everything in **English**.
  <!-- variant:blueprint -->
- **Change both variants together.** A change in behaviour goes into `kotlin-gradle/` *and*
  `java-maven/` in the same PR, with equivalent tests. Changes that only concern one language's idioms
  stay on that side. Models, forms, migrations and `application.yaml` exist in both variants and must
  be byte-identical — copy your change over; the `Blueprint Checks` workflow fails otherwise. See
  [ADR-0013](docs/adr/0013-two-stack-variants-side-by-side-on-main.md).
  <!-- /variant:blueprint -->
- **Keep the gates green.** The architecture, contract-drift and mutation (≥ 80) gates run in CI on
  every PR. They are fitness functions, not style guides — a violation fails the
  build. The mutation gate is **diff-scoped** on PRs (only the classes you changed); the full-module
  gate-80 sweep runs nightly.
- **Add tests.** This is a TDD codebase; match the test style to the layer (see `AGENTS.md`).
  Mutation testing means a test that runs without asserting will fail CI.
- **Changing the API?** Regenerate and commit `openapi/openapi.json` in the same change so the drift
  gate stays green. Service tasks are external-topic `@ProcessEngineWorker` beans — never introduce an
  embedded delegate.
- **Changing the database schema?** Flyway owns it. Add a new forward-only migration
  `V{n}__description.sql` under `service/app/src/main/resources/db/migration/` in the same change as
  the entity edit — never edit an already-applied migration. Hibernate runs `validate`, so a mismatch
  fails startup. A dev database first created by the old `ddl-auto: create` has no Flyway history;
  reset it once with `docker compose -f stack/docker-compose.yml down -v` before running. See
  [ADR-0010](docs/adr/0010-flyway-for-database-migrations.md).

## Before opening a PR

<!-- variant:kotlin-gradle -->
```bash
(cd kotlin-gradle && ./gradlew build && ./gradlew :service:app:pitest)   # mutation score >= 80
```
<!-- /variant:kotlin-gradle -->
<!-- variant:java-maven -->
```bash
(cd java-maven && ./mvnw verify \
  && ./mvnw -pl service/app -am test-compile pitest:mutationCoverage)   # mutation score >= 80
```
<!-- /variant:java-maven -->

```bash
git diff --exit-code openapi/openapi.json    # the API contract must not drift
npm ci && npm run lint:bpmn                   # the BPMN models lint clean
```

All of these run in CI on every pull request (JDK 21 / Node ≥ 22.12).

## Reporting bugs / requesting features

Open an issue. For a process- or contract-related bug, attaching the relevant `.bpmn` model or the
`openapi.json` diff is the fastest path to a fix.
