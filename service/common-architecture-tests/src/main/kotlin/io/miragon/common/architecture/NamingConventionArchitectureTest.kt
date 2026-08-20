package io.miragon.common.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests
import com.tngtech.archunit.lang.conditions.ArchConditions.haveSimpleNameEndingWith
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Naming conventions per hexagonal layer — the ArchUnit part of the combined suite (a simple
 * bytecode name check, so ArchUnit owns it).
 *
 * Each rule lists the class-name suffixes allowed in a given package with a short rationale via
 * [AllowedSuffix], so the file doubles as living documentation. Packages a given service does not
 * have simply match no classes (`allowEmptyShould(true)`), so the same rule set fits every service.
 * The generated `adapter.process` package is intentionally left unchecked.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class NamingConventionArchitectureTest(
    val rootPackage: String,
) {

    private val productionClasses =
        ClassFileImporter()
            .withImportOption(DoNotIncludeTests())
            .importPackages(rootPackage)

    @Nested
    inner class Ports {

        @Test
        fun `inbound ports are use-cases or queries`() {
            checkNaming(
                packageRoot = "..application.port.inbound",
                allowedSuffixes =
                    listOf(
                        AllowedSuffix("UseCase", "inbound port for a state-changing operation"),
                        AllowedSuffix("Query", "inbound port for a read-only operation"),
                    ),
            )
        }

        @Test
        fun `outbound ports are ports or repositories`() {
            checkNaming(
                packageRoot = "..application.port.outbound",
                allowedSuffixes =
                    listOf(
                        AllowedSuffix("Port", "generic outbound port delegating to infrastructure"),
                        AllowedSuffix("Repository", "outbound port for persistence access"),
                        AllowedSuffix("Process", "outbound port driving the process engine"),
                    ),
            )
        }
    }

    @Nested
    inner class Application {

        @Test
        fun `application services are services`() {
            checkNaming(
                packageRoot = "..application.service",
                allowedSuffixes =
                    listOf(
                        AllowedSuffix("Service", "orchestrates a use case and owns the transaction boundary"),
                        AllowedSuffix("Configuration", "Spring configuration for the application layer"),
                    ),
            )
        }
    }

    @Nested
    inner class InboundAdapters {

        @Test
        fun `rest adapters follow naming conventions`() {
            checkNaming(
                packageRoot = "..adapter.inbound.rest",
                allowedSuffixes =
                    listOf(
                        AllowedSuffix("Controller", "Spring MVC REST controller"),
                        AllowedSuffix("Dto", "REST response type outside the domain model"),
                        AllowedSuffix("Input", "REST request type"),
                        AllowedSuffix("Mapper", "translates between REST DTOs and domain types"),
                        AllowedSuffix("Configuration", "Spring web configuration for the REST adapter (CORS, OpenAPI metadata, error handling)"),
                    ),
            )
        }

        @Test
        fun `cibseven adapters follow naming conventions`() {
            checkNaming(
                packageRoot = "..adapter.inbound.cibseven",
                allowedSuffixes =
                    listOf(
                        AllowedSuffix("Delegate", "JavaDelegate invoked by a BPMN service task"),
                        AllowedSuffix("Worker", "external-task worker subscribed to a BPMN topic"),
                    ),
            )
        }
    }

    @Nested
    inner class OutboundAdapters {

        @Test
        fun `outbound adapters follow naming conventions`() {
            checkNaming(
                packageRoot = "..adapter.outbound",
                allowedSuffixes =
                    listOf(
                        AllowedSuffix("PersistenceAdapter", "primary outbound adapter implementing out-ports"),
                        AllowedSuffix("Adapter", "outbound adapter adapting to infrastructure"),
                        AllowedSuffix("Mapper", "translates between infrastructure types and domain objects"),
                        AllowedSuffix("Entity", "JPA entity mapped to a database table"),
                        AllowedSuffix("Repository", "Spring Data repository backing a persistence adapter"),
                    ),
            )
        }
    }

    private fun checkNaming(
        packageRoot: String,
        allowedSuffixes: List<AllowedSuffix>,
    ) {
        require(allowedSuffixes.isNotEmpty()) { "allowedSuffixes must not be empty" }
        val nameCondition =
            allowedSuffixes
                .map { haveSimpleNameEndingWith(it.suffix) }
                .reduce { acc, next -> acc.or(next) }

        ArchRuleDefinition
            .classes()
            .that()
            .resideInAPackage("$packageRoot..")
            .and()
            .areTopLevelClasses()
            .and()
            .areNotAnonymousClasses()
            .and()
            .haveSimpleNameNotEndingWith("Kt")
            .and()
            .haveSimpleNameNotEndingWith("_")
            .and()
            .haveSimpleNameNotContaining("\$")
            .should(nameCondition)
            .allowEmptyShould(true)
            .check(productionClasses)
    }

    /**
     * An allowed class-name suffix together with a short rationale.
     * The [reason] documents *why* a suffix is allowed; it is not part of ArchUnit's failure message.
     */
    data class AllowedSuffix(
        val suffix: String,
        val reason: String,
    )
}
