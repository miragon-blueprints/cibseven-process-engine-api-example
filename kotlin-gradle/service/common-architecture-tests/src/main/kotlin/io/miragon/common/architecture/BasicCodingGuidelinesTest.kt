package io.miragon.common.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * ArchUnit half of the combined suite: general coding guidelines that rely on the resolved bytecode
 * graph (package structure, freedom of cycles, no `println`).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BasicCodingGuidelinesTest(
    val pathFromRoot: String,
) {

    private var allClasses =
        ClassFileImporter()
            .importPackages(pathFromRoot)

    private val productionClasses =
        ClassFileImporter()
            .withImportOption(DoNotIncludeTests())
            .importPackages(pathFromRoot)

    @Test
    fun `each class has package declaration`() {
        ArchRuleDefinition
            .classes()
            .should()
            .resideInAnyPackage("$pathFromRoot..")
            .because("All classes should be in the specified package structure")
            .check(allClasses)
    }

    @Test
    fun `classes are free of cycles`() {
        SlicesRuleDefinition
            .slices()
            .matching("$pathFromRoot.(**)")
            .should()
            .beFreeOfCycles()
            .because("Classes should not have circular dependencies")
            .check(allClasses)
    }

    @Test
    fun `production code does not use println or System out`() {
        ArchRuleDefinition
            .noClasses()
            .should()
            .callMethod(java.io.PrintStream::class.java, "println", Any::class.java)
            .because("Use a logger instead of println or System.out for diagnostic output")
            .check(productionClasses)
    }
}
