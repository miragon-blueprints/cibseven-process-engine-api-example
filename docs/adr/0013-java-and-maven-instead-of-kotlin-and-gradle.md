# 0013 — Java and Maven instead of Kotlin and Gradle

- **Status:** Proposed
- **Date:** 2026-10-05

## Context

The backend was written in **Kotlin 2.4** and built with **Gradle 9.5** (Kotlin DSL, a
`gradle/libs.versions.toml` version catalog, the `./gradlew` wrapper). Several parts of the toolchain
were Kotlin-specific: the Kotlin compiler plugins (`kotlin-spring`, `kotlin-jpa`, kapt), the
bpmn-to-code Gradle plugin generating Kotlin `*ProcessApi` objects, Konsist for the source-level
architecture rules ([ADR-0007](0007-two-architecture-test-tools-archunit-and-konsist.md)), MockK /
springmockk / mockito-kotlin for test doubles, and kotlin-logging. ADRs 0002–0004 and 0008–0011 name
these tools and commands.

The engine-facing libraries — process-engine-api, process-engine-worker, the CIB seven adapter and the
bpmn-to-code runtime/testing modules — are themselves written in Kotlin and are not part of this
change; they are consumed through their Java-facing API.

> TODO(maintainers): add the motivation for the switch.

## Decision

We write the backend in **Java 21** and build it with **Maven** through the **Maven wrapper**
(`./mvnw`, Maven 3.10.0, script-only `.mvn/wrapper`). The port is a translation, not a redesign: same
packages and class names, REST contract, BPMN behaviour, architecture rules, test cases and gates.
The language level is 21, the same as the former Kotlin `jvmTarget` and the buildpack's
`BP_JVM_VERSION`; moving to Java 25 is a separate bump under
[ADR-0008](0008-track-the-latest-major-versions.md).

- **Build:** the root `pom.xml` (parent `spring-boot-starter-parent`) is a reactor of
  `service/common-architecture-tests` and `service/app`; versions are properties in the root POM
  instead of a version catalog. Code compiles with `--release 21 -parameters` (Spring MVC parameter
  names and the worker's `@Variable` binding rely on it); Surefire forks one JVM per test class, as
  Gradle's `forkEvery = 1` did.
- **Generated process API:** the `bpmn-to-code-maven` plugin (`outputLanguage = JAVA`) writes
  `*ProcessApi.java` into `src/main/java/…/adapter/process` in the `generate-sources` phase.
- **Tests:** JUnit 5 + AssertJ + **Mockito** (`@MockitoBean` in Spring slices) replace MockK,
  springmockk and mockito-kotlin. Backtick test names become snake_case methods, rendered with spaces by
  JUnit's `ReplaceUnderscores` display-name generator; the named-argument test factory becomes the
  fluent `TestObjectBuilder`.
- **Logging:** SLF4J replaces kotlin-logging, with the same levels and messages.
- **Code conventions:** `record`s for value objects, aggregates, port commands/results and REST DTOs;
  a port or service method that may find nothing returns **`Optional<T>`** (Kotlin `T?`), while
  nullable record components stay plain references documented in Javadoc. Exception types and messages
  are kept (`require` → `IllegalArgumentException`, `check`/`error` → `IllegalStateException`) because
  the REST advice maps types to HTTP statuses.
- **Architecture tests:** Konsist only reads Kotlin, so JavaParser takes over the source rules —
  [ADR-0014](0014-two-architecture-test-tools-archunit-and-javaparser.md).
- **The OpenAPI contract is preserved** ([ADR-0003](0003-openapi-as-the-checked-in-contract.md)) apart
  from four `"content" : { }` entries on the `200` responses of the bodyless `POST`s (`withdraw`,
  `sign-contract`, `report-handover`, `clarify-alternative`). Those endpoints return
  `ResponseEntity<Void>`, for which springdoc omits `content`; Kotlin's `ResponseEntity<Unit>` rendered
  an empty object. Both say "no body", so we accept the diff instead of emulating it.
- **The mutation gate stays at 80** with the same targets and exclusions
  ([ADR-0004](0004-mutation-testing-as-a-blocking-pr-gate.md)). The denominator shrinks: the Kotlin
  baseline's 85 % (174/205) included about 117 mutants in Kotlin-generated getters and constructors
  (over the hand-written logic alone it was about 83 %, 73/88), while PIT's default record filter
  skips record accessors and canonical/compact constructors altogether. That removes boilerplate noise
  but also real logic: the value records' validation moves out of the gate (see Consequences).

## Consequences

- **Positive:** one language in the code base and a plain JDK + Maven toolchain — no Kotlin compiler
  plugins (`kotlin-spring`, `kotlin-jpa`, kapt) to keep in step with Spring and JPA. Mutation results no
  longer contain Kotlin-synthetic equivalent mutants (`value class` null checks, safe-call mapping,
  `data class` accessors).
- **Negative / trade-offs:** more ceremony — explicit constructors, entity getters, no default or named
  arguments (callers pass `null` explicitly, tests use `TestObjectBuilder`); nullability is a convention
  (Javadoc + `Optional`), not checked by the compiler. The Kotlin libraries read less naturally from
  Java (`getX()` accessors, no default arguments, `Empty.INSTANCE`, the checked `BpmnErrorOccurred` in
  worker signatures). Module-scoped Maven runs need `-pl service/app -am` and class-name test selection.
  The value records' compact-constructor validation (`BikeId`, `OrderId`, `ContractId`, `CustomerName`,
  `Email`) is outside the mutation gate: Kotlin's `require` in `init` was mutated, the record's
  `if … throw` is not, so a PR touching only those checks passes the gate without being graded on them.
  They are guarded solely by the domain tests' exact `hasMessage` assertions.
- **Neutral:** kotlin-stdlib and kotlin-reflect stay on the runtime classpath transitively (pinned via
  `kotlin-bom`), because the engine libraries need them. Build output moves from `build/` to `target/`
  (boot jar `service/app/target/app-1.0-SNAPSHOT.jar`, PIT report `service/app/target/pit-reports`).
  Dependabot watches the `maven` ecosystem. ADR-0007 is superseded by ADR-0014; the other ADRs that
  name a Gradle or Kotlin command, file or tool are amended in place, their decisions unchanged.

## Implementation notes

| Gradle (before) | Maven, from the repo root |
|---|---|
| `./gradlew build` | `./mvnw verify` |
| `./gradlew :service:app:test --tests "*FooTest"` | `./mvnw -pl service/app -am test -Dtest='*FooTest' -Dsurefire.failIfNoSpecifiedTests=false` |
| `./gradlew :service:app:bootRun` | `./mvnw -pl service/app -am spring-boot:run` |
| `./gradlew generateBpmnModels` | `./mvnw -pl service/app generate-sources` |
| `./gradlew :service:app:pitest [-PmutationTargetClasses=…]` | `./mvnw -pl service/app -am test-compile pitest:mutationCoverage [-DmutationTargetClasses=…]` |
| `./gradlew :service:app:bootBuildImage` | `./mvnw -pl service/app -am -DskipTests package spring-boot:build-image-no-fork` |

- `-am` builds `common-architecture-tests` in the same reactor, so the build never needs
  `./mvnw install`. With `-am`, `-Dtest=…` needs `-Dsurefire.failIfNoSpecifiedTests=false`, and a pattern that matches
  nothing then passes silently — read the `Tests run:` line.
- A package glob such as `-Dtest='io.miragon.blueprint.architecture.*'` runs **0** architecture tests:
  Surefire reports the `@Nested` rule classes under their declaring package
  (`io.miragon.common.architecture`). Select `-Dtest=ArchitectureTest`.
- `spring-boot:build-image` would fork `package` *with* tests; `bootBuildImage` ran none, hence
  `-DskipTests package spring-boot:build-image-no-fork`.
- `OpenApiSpecExportTest` and `JavaSourceGuidelinesTest` locate the repo root by its `.mvn/` directory,
  so `.mvn/` must stay committed. `.mvn/maven.config` turns off transfer progress for every run (local
  and CI); `.mvn/wrapper/maven-wrapper.properties` pins the distribution's `distributionSha256Sum`,
  which must be updated together with `distributionUrl` on every Maven bump.
- Maven resolves version conflicts nearest-wins (Gradle: highest-wins). The root POM therefore imports
  `kotlin-bom` and `spring-boot-dependencies` before the process-engine-adapter BOM and pins the few
  transitive versions that would otherwise differ from what the Gradle build resolved.
