package io.miragon.common.architecture

import com.tngtech.archunit.core.domain.JavaClass.Predicates.INTERFACES
import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition
import com.tngtech.archunit.library.Architectures
import io.miragon.common.architecture.condition.InterfaceImplementationConditions
import io.miragon.common.architecture.condition.UseCaseDependencyConditions.onlyFulfilOneUseCase
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * ArchUnit half of the combined suite: the *dependency & structure* rules, checked against the
 * resolved bytecode graph. Naming conventions live in [NamingConventionArchitectureTest].
 *
 * The `adapter.process` package holds the **generated** BPMN process API and process-engine
 * configuration. It is a technical seam that does not fit the inbound/outbound split, so it is
 * excluded from the adapter-location rule and ignored by the layered-architecture completeness check.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class HexagonalArchitectureTest(
    val rootPackage: String,
) {

    private var allClasses =
        ClassFileImporter()
            .importPackages(rootPackage)

    private val productionClasses =
        ClassFileImporter()
            .withImportOption(DoNotIncludeTests())
            .importPackages(rootPackage)

    @Test
    fun `hexagonal architecture should be respected`() {
        val architectureRule =
            Architectures
                .layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("Domain")
                .definedBy("..domain..")
                .layer("In-Ports")
                .definedBy("..application.port.inbound..")
                .layer("Out-Ports")
                .definedBy("..application.port.outbound..")
                .layer("In-Adapters")
                .definedBy("..adapter.inbound..")
                .layer("Out-Adapters")
                .definedBy("..adapter.outbound..")
                .layer("Application")
                .definedBy("..application.service..")
                .whereLayer("In-Ports")
                .mayOnlyBeAccessedByLayers("Application", "In-Adapters")
                .whereLayer("Out-Ports")
                .mayOnlyBeAccessedByLayers("Application", "Out-Adapters")
                .whereLayer("In-Adapters")
                .mayNotBeAccessedByAnyLayer()
                .whereLayer("Out-Adapters")
                .mayNotBeAccessedByAnyLayer()
                .whereLayer("Application")
                .mayNotBeAccessedByAnyLayer()
                .whereLayer("Domain")
                .mayNotAccessAnyLayer()
                .ensureAllClassesAreContainedInArchitectureIgnoring(
                    rootPackage,
                    "$rootPackage.architecture",
                    "$rootPackage.adapter.process..",
                )

        // Production code only: test classes (process tests, fixtures) live outside the layers.
        architectureRule.check(productionClasses)
    }

    @Nested
    inner class DomainTests {

        @Test
        fun `domain layer is technology neutral`() {
            ArchRuleDefinition
                .classes()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .onlyDependOnClassesThat()
                .resideInAnyPackage(*DOMAIN_ALLOWED_PACKAGES)
                .because("The domain must not depend on any framework or infrastructure code")
                .check(productionClasses)
        }
    }

    @Nested
    inner class GeneralPortTests {

        @Test
        fun `application ports should be interfaces`() {
            ArchRuleDefinition
                .classes()
                .that()
                .resideInAPackage("..application.port.inbound..")
                .or()
                .resideInAPackage("..application.port.outbound..")
                .and()
                .areTopLevelClasses()
                .should()
                .beInterfaces()
                .because("All ports should be defined as interfaces")
                .check(allClasses)
        }

        @Test
        fun `ports should not depend on services`() {
            ArchRuleDefinition
                .noClasses()
                .that()
                .resideInAPackage("..application.port..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..application.service..")
                .because("Ports decouple adapters from the application services and must stay independent")
                .check(allClasses)
        }
    }

    @Nested
    inner class ApplicationTests {

        @Test
        fun `application layer only orchestrates`() {
            ArchRuleDefinition
                .classes()
                .that()
                .resideInAPackage("..application..")
                .should()
                .onlyDependOnClassesThat()
                .resideInAnyPackage(*APPLICATION_ALLOWED_PACKAGES)
                .because("The application layer only orchestrates; infrastructure belongs in adapters")
                .check(productionClasses)
        }

        @Test
        fun `application classes reside in service or port sub-packages`() {
            ArchRuleDefinition
                .classes()
                .that()
                .resideInAPackage("..application..")
                .should()
                .resideInAnyPackage("..application.service..", "..application.port..")
                .because("The application layer is structured into services and ports")
                .check(allClasses)
        }

        @Test
        fun `application service should implement exactly one use-case`() {
            ArchRuleDefinition
                .classes()
                .that()
                .resideInAPackage("..application.service..")
                .should(InterfaceImplementationConditions.implementExactlyOneInterfaceFrom("application.port.inbound"))
                .because("application services must implement at least one inbound port")
                .check(productionClasses)
        }

        @Test
        fun `application service should not depends on other application services`() {
            ArchRuleDefinition
                .noClasses()
                .that()
                .resideInAPackage("..application.service..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..application.service..")
                .because("Application services should not depend on each other")
                .check(productionClasses)
        }

        @Test
        fun `application service should not use any inbound port`() {
            ArchRuleDefinition
                .noClasses()
                .that()
                .resideInAPackage("..application.service..")
                .should()
                .accessClassesThat(
                    resideInAPackage("..application.port.inbound..").and(INTERFACES),
                ).because("A service may implement its own use-case but must never call another one")
                .check(productionClasses)
        }
    }

    @Nested
    inner class AdapterTests {

        @Test
        fun `adapter classes reside in inbound or outbound sub-packages`() {
            ArchRuleDefinition
                .classes()
                .that()
                .resideInAPackage("..adapter..")
                .and()
                .resideOutsideOfPackage("..adapter.process..")
                .should()
                .resideInAnyPackage("..adapter.inbound..", "..adapter.outbound..")
                .because("Adapters are either inbound (driving) or outbound (driven)")
                .check(allClasses)
        }

        @Test
        fun `out adapters should not reference in ports`() {
            ArchRuleDefinition
                .noClasses()
                .that()
                .resideInAPackage("..adapter.outbound..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..application.port.inbound..")
                .because("Outbound adapters are driven and must not reach into inbound ports")
                .check(allClasses)
        }

        @Test
        fun `in adapters should not implement out ports`() {
            ArchRuleDefinition
                .noClasses()
                .that()
                .resideInAPackage("..adapter.inbound..")
                .should()
                .implement(resideInAPackage("..application.port.outbound.."))
                .because("Inbound adapters are driving and must not implement outbound ports")
                .check(allClasses)
        }

        @Test
        fun `in adapters should only offer one use-case or query`() {
            ArchRuleDefinition
                .classes()
                .that()
                .resideInAPackage("..adapter.inbound..")
                .should(onlyFulfilOneUseCase)
                .because("In-adapters should implement exactly one use-case or query")
                .check(productionClasses)
        }
    }

    private companion object {

        /** Packages the domain layer is allowed to depend on — pure language & serialization, no infrastructure. */
        val DOMAIN_ALLOWED_PACKAGES =
            arrayOf(
                "..domain..",
                "java..",
                "kotlin..",
                "kotlinx..",
                "org.jetbrains..",
                "", // allows the usage of primitive types
            )

        /** Packages the application layer is allowed to depend on — domain, ports and orchestration tooling. */
        val APPLICATION_ALLOWED_PACKAGES =
            arrayOf(
                "..domain..",
                "..application..",
                "java..",
                "kotlin..",
                "kotlinx..",
                "org.jetbrains..",
                "mu..",
                "io.github.microutils..",
                "org.springframework..",
                "com.fasterxml.jackson..",
                "", // allows the usage of primitive types
            )
    }
}
