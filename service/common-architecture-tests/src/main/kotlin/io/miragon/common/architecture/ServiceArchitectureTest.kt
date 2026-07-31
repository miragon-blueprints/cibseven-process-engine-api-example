package io.miragon.common.architecture

import org.junit.jupiter.api.Nested

/**
 * A single, ready-to-use architecture suite for a service — combining **ArchUnit** and **Konsist**,
 * each doing what it is best at.
 *
 * A service wires up the full suite with one line:
 *
 * ```kotlin
 * class ArchitectureTest : ServiceArchitectureTest("io.miragon.blueprint")
 * ```
 *
 * ### Why a mix, and who owns what
 *
 * - **ArchUnit** reads compiled **bytecode**, so it sees the fully resolved dependency graph. It owns
 *   the *dependency & structure* rules — hexagonal layering, technology-neutrality of domain &
 *   application, port/adapter isolation ([Dependencies]), naming conventions ([Naming]), freedom of
 *   cycles and the no-`println` check ([CodingGuidelines]).
 * - **Konsist** reads Kotlin **source**, so it sees things the compiler erases. It owns the
 *   *source-structure* rules — one top-level declaration per file (SRP) and no wildcard imports
 *   ([KotlinSource]).
 *
 * This module is **self-contained**: it carries both the ArchUnit and Konsist dependencies and its
 * own copies of the rules, so it can be dropped into a service as a single test dependency.
 */
abstract class ServiceArchitectureTest(
    private val rootPackage: String,
) {

    @Nested
    inner class Dependencies : HexagonalArchitectureTest(rootPackage)

    @Nested
    inner class Naming : NamingConventionArchitectureTest(rootPackage)

    @Nested
    inner class CodingGuidelines : BasicCodingGuidelinesTest(rootPackage)

    @Nested
    inner class KotlinSource : KotlinSourceGuidelinesTest(rootPackage)
}
