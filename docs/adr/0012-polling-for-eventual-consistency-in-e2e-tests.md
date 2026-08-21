# 0012 — Poll for eventual consistency in end-to-end tests

- **Status:** Accepted
- **Date:** 2026-08-21

## Context

Every command endpoint in this API is **asynchronous**. A command controller drives the embedded
CIB seven engine through the `process-engine-api` (start / correlate a message / complete a task) and
returns **`202 Accepted`** immediately — it waits for the publish/correlation ack, not for the effect.
The observable effect lands some unbounded time later: the async **job executor** drives the process to
its next wait state, an external-task `@ProcessEngineWorker` polls its topic, runs, and writes the read
model (`leasing_application`). The gap between "the command returned" and "its effect is visible" is
real, and it is **environment-dependent** — a loaded CI runner is slower than a laptop.

The Bruno end-to-end suite (`bruno/`) originally bridged that gap with **fixed sleeps** — 18 hand-tuned
`setTimeout`s from 1.5 s to a 10 s wait for the incident-retry window. That is the classic flaky-test
anti-pattern: a sleep tuned to pass locally loses the race under CI load, while the safe-side sleeps
waste minutes on every run. The same async shape is inherent to **every process blueprint** in this
family, so the fix has to be a reusable pattern, not a per-test number.

## Decision

We assert eventual state by **polling until the real condition holds, capped by a generous timeout** —
never by sleeping a guessed duration.

- **Shared helpers** live in `bruno/collection.bru` (a collection-level `script:pre-request`, so they
  are in scope for every request): `pollUntil(config, predicate, opts)` and the convenience wrappers
  `pollApp(path, predicate)` (GET the app read model) and `pollEngine(path, predicate)` (GET the
  CIB seven engine REST API). They return the instant the predicate is met and only wait the full
  budget when something is genuinely wrong — at which point the request's own assertions report the
  real, still-wrong state instead of a bare timeout.
- **Budgets are env-driven** (`pollTimeoutMs` / `pollIntervalMs` in the environment file), so a sibling
  blueprint with different propagation characteristics tunes them **once**, in one place. A scenario
  that needs a wider window (the incident-retry demo) passes `{ timeoutMs }` at the call site.
- **Each scenario polls for its own precondition or assertion**, mirroring the read model's observable
  fields — the `status` enum (`RECEIVED → ORDERED → HANDED_OVER → ACTIVE`, plus `WITHDRAWN`,
  `REJECTED`, `CANCELLED`) and worker-set fields (`contractId`, `orderId`). Command steps gate on the
  precondition that makes the command valid (e.g. `contractId != null` before `sign-contract`;
  `status == "ORDERED"` before `report-handover`; the inbox listing the item before completing the
  `clarify-alternative` user task). Read/assert steps poll the engine REST query (a timer job, a task,
  a history activity instance) until the resource the step asserts on exists.
- **Division of labour holds:** Bruno asserts the **synchronous request/response contract** (status
  codes, DTO shape); genuinely engine-level, deterministic checks (timer fast-forward, full token
  flow) stay in the JVM CIB seven process-test layer with Awaitility. See ADR-0004 for the test
  layering.
- **The Bruno CLI is pinned** (`@usebruno/cli@4.0.0`): the script sandbox's capabilities (available
  globals, the `require` whitelist the helpers depend on) can change between majors, so an unpinned
  `latest` is a correctness risk, not just a supply-chain one.

## Consequences

- **Positive:** the suite is robust under CI load and *faster* in the common case — it waits exactly as
  long as the engine needs, turning the 10 s incident sleep into "as soon as the incident appears".
  One env-tunable budget replaces 18 magic numbers, and the pattern ports to every sibling blueprint.
- **Negative / trade-offs:** each polled command step issues an extra read before the "official"
  request; a predicate must be kept honest (it should mirror what the request asserts, or it silently
  waits out the whole budget). Two read-model writes — `report-handover` and `withdraw` — set their
  status (`HANDED_OVER` / `WITHDRAWN`) *unconditionally*, independent of message correlation, so tests
  assert the **downstream** worker-driven state (`ACTIVE`, `CANCELLED`, the cancellation end event) as
  proof the process actually advanced, not those intermediate flips.
- **Neutral:** the "submit → poll an observable status" shape becomes the documented client contract
  for these async APIs, for real consumers as much as for tests.

## Implementation notes

- The Bruno script sandbox exposes **no global `fetch`**; `require('axios')` works and is what the
  helpers use. Helpers are attached to `globalThis` in the collection script so request-level scripts
  can call them.
- Message commands correlate by the global `correlationKey` (the application id). A command sent before
  the wait state is open surfaces the engine's `MismatchingMessageCorrelationException` as a `409`, so
  gating on the read-model precondition (rather than exact timing) is what keeps the `202` valid.
  `clarify-alternative` completes a user task resolved from the delivery pool, hence gating on the
  inbox listing the item.
- **Deferred, not adopted:** a test-only endpoint exposing the engine's last-processed job/command
  position (letting a test assert "read model ≥ my command" deterministically) would be the strongest
  guarantee, but it leaks engine internals into the API surface for test convenience. Revisit only if
  predicate-based polling proves insufficient.
