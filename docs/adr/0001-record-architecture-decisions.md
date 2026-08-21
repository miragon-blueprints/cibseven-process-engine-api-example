# 0001 — Record architecture decisions

- **Status:** Accepted
- **Date:** 2026-08-18

## Context

This repository is a **blueprint** — a starting point meant to be forked and made your own. Its value
is not only the working code but the *reasoning* behind its shape: why the backend is hexagonal, why
service tasks are external-topic workers rather than embedded delegates, why the OpenAPI spec is
committed. That reasoning normally lives in people's heads and pull-request threads, where a new
contributor (human or AI agent) cannot find it and where it decays as the team changes.

## Decision

We record architecturally significant decisions as **Architecture Decision Records (ADRs)**, following
Michael Nygard's format, one Markdown file per decision under `docs/adr/`, numbered sequentially from
`0001`. Each record captures Status, Context, Decision, and Consequences. New records are copied from
[`0000-adr-template.md`](0000-adr-template.md) and never renumbered; a superseded decision is marked
`Deprecated`/`Superseded by` rather than deleted, so the history stays legible.

An ADR is committed **in the same commit** as the code it explains, so the decision and its
implementation are traceable together in `git` history.

## Consequences

- **Positive:** a forker inherits the *why*, not just the *what*; agents can read the constraints
  before touching code; decisions are reviewable in PRs like any other change.
- **Negative / trade-offs:** a small discipline cost — a real decision now means writing a paragraph.
- **Neutral:** `docs/adr/` becomes the canonical index of cross-cutting decisions; each ADR carries
  its own detail in its Decision section rather than delegating to a separate long-form guide.
