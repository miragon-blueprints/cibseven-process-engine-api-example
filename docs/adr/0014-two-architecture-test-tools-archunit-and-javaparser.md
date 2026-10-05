# 0014 — Two architecture-test tools: ArchUnit (bytecode) and JavaParser (source)

- **Status:** Accepted
- **Date:** 2026-10-05

## Context

The hexagonal rules are machine-enforced ([ADR-0002](0002-hexagonal-architecture-for-the-backend.md)),
and the suite that enforces them runs **two** tools. [ADR-0007](0007-two-architecture-test-tools-archunit-and-konsist.md),
which this record supersedes, paired ArchUnit with Konsist. Konsist only reads **Kotlin** source, so after the switch to Java
([ADR-0013](0013-java-and-maven-instead-of-kotlin-and-gradle.md)) it no longer sees a single file. The
reason for two tools is unchanged — it comes down to what each tool can *see*:

- **ArchUnit** inspects compiled **bytecode**. It reasons about the resolved type and dependency graph
  — which package depends on which, whether a class is an interface, how many constructor parameters of
  a given type a class has. It cannot see facts that compilation erases: every top-level type becomes a
  class file of its own, linked to its source only by the optional `SourceFile` attribute, and imports
  do not survive at all.
- **JavaParser** parses Java **source** into an AST. It sees exactly those source-level facts — files,
  imports, top-level type declarations — that ArchUnit has lost. Used without symbol resolution, as
  here, it is not the right tool for reasoning about the resolved dependency graph.

Each is blind to the other's domain; neither alone covers both.

## Decision

We use **both** ArchUnit and **JavaParser** (`javaparser-core`), each for the rules only it can
express, in `service/common-architecture-tests` (all fail `./mvnw verify`):

- **ArchUnit (bytecode)** — the layered-dependency graph and naming/suffix rules
  (`HexagonalArchitectureTest`, `NamingConventionArchitectureTest`) plus basic coding guidelines
  (`BasicCodingGuidelinesTest`: no `println`/`System.out`, no package cycles).
- **JavaParser (source)** — the same two conventions Konsist enforced, in `JavaSourceGuidelinesTest`:
  at most one top-level type (class, interface, enum, record or annotation) per file, and no wildcard
  imports (except `java.util`, static imports included).

Both kinds of rule skip the generated `adapter.process` sources. `ServiceArchitectureTest` bundles all
four as `@Nested` classes, so a service still wires in the whole suite with one subclass
(`ArchitectureTest`).

## Consequences

- **Positive:** each rule is written against the representation where it is natural and cheap — no
  contorting a source-shape rule through bytecode, or a dependency rule through text. JavaParser is a
  plain parser library; checking source shape needs no compiler plugin and no classpath.
- **Negative / trade-offs:** two libraries to learn and keep current. JavaParser has no assertion DSL
  like Konsist's, so the two source rules are hand-written JUnit tests that walk the AST — file
  discovery, filtering and failure messages are our own code.
- **Neutral:** this is the deliberate **ceiling**, not a starting point. New structural rules go into
  whichever of these two fits — we do **not** add a third architecture/guardrail framework on top.

## Implementation notes

- JavaParser's default language level is Java 11, which cannot parse records; the suite sets
  `LanguageLevel.JAVA_21`. A file that does not parse fails the test instead of being skipped.
- Sources are found from the project root — the nearest directory at or above `user.dir` that contains
  `.mvn/` — skipping hidden directories and `target`, `build` and `node_modules`. The import rule
  covers every Java file of every module, main and test; the one-type rule covers the files under the
  service's root package.
- Surefire reports the nested rules under their declaring package (`io.miragon.common.architecture`),
  so run them with `-Dtest=ArchitectureTest`; a package glob on `io.miragon.blueprint.architecture`
  matches nothing.
