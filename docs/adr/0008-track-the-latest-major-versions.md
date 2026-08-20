# 0008 — Deliberately track the latest major versions

- **Status:** Accepted
- **Date:** 2026-08-20

## Context

The stack sits on the newest major of nearly everything at once — Spring Boot 4 and Kotlin 2.x on the
CIB seven Spring-Boot-4 line. That is a real choice with a real cost: newest majors have smaller
ecosystems, more breaking-change churn, and occasionally force an integration workaround (e.g.
excluding the non-SB4 CIB seven webclient so the app starts on Spring 7). A fork left untouched for
months may need an upgrade pass before it builds again.

Left unwritten, this is indistinguishable from *drift* — a reader can't tell whether being on the bleeding
edge is a stance or an accident. This ADR makes it a stance.

## Decision

We **deliberately track the latest stable major versions** across the whole stack, and accept the upkeep.

The reason is the template's job: these are **solution templates** that exist to show what the *current*
technological stand makes possible — including first-class support for **AI-driven / agentic development**,
which the newest tooling plus our guardrails ([ADR-0002](0002-hexagonal-architecture-for-the-backend.md)
architecture tests, [ADR-0004](0004-mutation-testing-as-a-blocking-pr-gate.md) mutation gate, typed
contracts, linting) are meant to enable. A template pinned to yesterday's versions would misrepresent both
the capabilities and the starting point we actually recommend.

Mechanics that make this safe rather than reckless: dependency updates are automated (Dependabot), every
version is exact-pinned, and each bump runs the full gate set (build, mutation ≥ 80, contract drift), so
updates can be taken continuously instead of in a scary big-bang. Bleeding-edge workarounds are
documented at the point of use.

## Consequences

- **Positive:** the template reflects current best practice and the newest capabilities; a forker starts
  modern instead of mid-migration; AI-assisted development is exercised against today's tooling and
  guardrails, which is the whole point.
- **Negative / trade-offs:** a higher maintenance cadence and occasional early-adopter friction (thinner
  community answers, integration hacks); a long-dormant fork may need an upgrade pass before it builds.
  We accept these as the price of the stance.
- **Neutral:** "latest **stable** major" — we track releases, not RCs/pre-releases, and the CI gates, not a
  calendar, decide whether a given bump lands.
