# 0004 — Mutation testing as a blocking PR gate

- **Status:** Accepted
- **Date:** 2026-10-05

## Context

Line coverage answers "was this line executed?", not "would a test notice if the code broke?". That
gap matters most for **AI-assisted tests**, which reliably chase coverage while writing weak assertions
(assert-not-null instead of assert-a-value). A blueprint that asks agents to generate tests needs a
gate that grades *assertion strength*, not just execution. Mutation testing is inherently slower than
unit tests, though, so the gate must stay a guardrail, not a tollbooth — it must not become the thing
that stalls merges.

## Decision

We run **PIT (pitest)** as a **blocking gate** with `mutationThreshold = 80`, configured in
`service/app/pom.xml` (`pitest-maven`) and run via
`./mvnw -pl service/app -am test-compile pitest:mutationCoverage`.

- **On PRs it runs diff-scoped.** `.github/workflows/pre-merge.yml` computes the backend `*.java` files
  the PR changed (from `pull_request.base.sha`), maps them to `io.miragon.blueprint.<pkg>.<File>*`, and
  passes them to pitest via `-DmutationTargetClasses`. It blocks the PR on the changed classes' score
  but stays off the critical path, and is skipped when a PR touches no backend code.
- **Nightly runs the full sweep.** `.github/workflows/nightly.yml` mutates the whole
  `io.miragon.blueprint.*` module with no property override — the authoritative gate-80 run — and
  uploads the HTML report as an artifact.
- Both runs **exclude noise**: the generated `*ProcessApi`, the Spring bootstrap and
  `BikeCatalogueSeeder`, and the `adapter.inbound.cibseven.*` workers (thin glue exercised only by the
  slow engine tests).
- The **kill-set** is the fast Mockito / `@WebMvcTest` / `@DataJpaTest` unit tests; the JGiven engine
  integration tests (`process.*`) and the ArchUnit/JavaParser tests (`architecture.*`) are excluded from
  the kill-set — they'd make every run slow and non-deterministic without adding mutation signal.

**Why 80 and not 100:** some mutants cannot be killed at a reasonable cost. An *equivalent* mutant
behaves exactly like the original, so no test can tell them apart; others are only distinguishable by
a test nobody should write — e.g. the `attempt < TASK_LOOKUP_ATTEMPTS` boundary of the
clarify-alternative task lookup in `LeasingProcessAdapter`, which only a test that waits out the whole
10-second lookup would notice. The PR gate scores each changed class on its own, and in a small class a
single such mutant is already a large share of the score: 100 would push authors to contort honest
code or exclude it, while 80 still fails a test that asserts too little. 80 is the honest bar.

**What the gate does not see:** PIT's default record filter skips record accessors and the record's
canonical/compact constructor. The validation in the value records (`BikeId`, `OrderId`, `ContractId`,
`CustomerName`, `Email`) therefore sits outside the gate — only the blank-check predicate lambdas the
id/name records pass to `chars().allMatch(…)` are mutated, as synthetic methods of their own. The
compact constructors' `if … throw` is guarded solely by the domain tests' exact `hasMessage`
assertions.

**What a weak test looks like** (the two patterns the gate caught in this repo's own spike): a test
that asserts too few of a DTO's fields — PIT blanks the unasserted ones (`return ""`) and every test
stays green; and a test that exercises only one branch of a boolean — the *"always return true"*
mutant is then *equivalent* to the original. Both are fixed by asserting **every** mapped field and
**both** outcomes of each branch — one assertion per outcome, not per method.

## Consequences

- **Positive:** AI-generated tests are graded on whether they'd catch a real fault; weak assertions
  surface as surviving mutants with per-mutant, inline feedback — at PR time, on the code the PR
  changed.
- **Negative / trade-offs:** mutation testing is slower than unit tests, hence the diff-scoped PR run
  and the separate nightly full sweep. Gate-80 over a single changed class is stricter granularity than
  over the module, so a PR touching only a small class with one equivalent mutant can dip below 80
  (fix: assert every field and both branches, or exclude the class), and a second, differently-named
  top-level type in the same file is out of PR scope until the nightly sweep. Code PIT does not mutate
  (record accessors and canonical/compact constructors, see above) passes the gate vacuously: a PR that
  only changes the `if … throw` of a value record's validation is not graded on it (for `Email` the run
  reports `Generated 0 mutations`) and stays green. The threshold is capped below 100 by equivalent and
  practically unkillable mutants.
- **Neutral:** PIT mutates the bytecode `javac` emits, with its default filters (records, generated
  code): lambdas are mutated as the synthetic methods they compile to, record boilerplate is not.

## Implementation notes

- `service/app/pom.xml` defines the `mutationTargetClasses` property (default `io.miragon.blueprint.*`
  → full-module scope, used by the nightly sweep and local runs; the PR job overrides it with
  `-DmutationTargetClasses=…`) and sets `failWhenNoMutations = false` so a PR whose changed classes are
  all excluded/non-mutable doesn't fail the build.
- Do **not** rename the `Mutation testing (PIT, gate 80)` job in `pre-merge.yml` — it is the
  branch-protection required check.
