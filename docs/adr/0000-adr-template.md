# NNNN — Short title of the decision

- **Status:** Proposed | Accepted | Deprecated | Superseded by [ADR-XXXX](xxxx-....md)
- **Date:** YYYY-MM-DD
- **Deciders:** (roles / names, optional)

## Context

What is the problem, force, or situation that motivates a decision? State the constraints and the
options considered. Keep it factual — describe the world as it is, not the choice yet. Link related
ADRs where a decision depends on or refines another.

## Decision

What we decided, in one or two sentences of active voice ("We use …", "We generate …"). Follow with
the concrete mechanics: which tool, which file, which gate enforces it.

## Consequences

What becomes true once this is in place — the good, the bad, and the merely different.

- **Positive:** what gets easier or safer.
- **Negative / trade-offs:** what gets harder, or what we accept as a cost.
- **Neutral:** what simply changes.

## Implementation notes (optional)

Concrete gotchas, wiring details, or non-obvious mechanics worth recording next to the decision.
Always the **last** section, and omitted entirely when there is nothing to add.

<!--
How to use this template:
- Copy it to docs/adr/NNNN-kebab-case-title.md, NNNN = highest existing number + 1, zero-padded.
- Keep an ADR to roughly one screenful. One decision per record.
- Commit the ADR in the same commit as the change it documents, so decision and code travel together.
- An ADR always describes the **current** state as if it had always been so — write it in the present
  tense, and never add "Update", "Changelog", or dated-amendment sections. When a decision changes,
  rewrite the record in place (bump the Date) or supersede it with a new ADR and flip this one's Status
  to "Superseded by …". The reader should never have to reconstruct the current state from a diff.
-->
