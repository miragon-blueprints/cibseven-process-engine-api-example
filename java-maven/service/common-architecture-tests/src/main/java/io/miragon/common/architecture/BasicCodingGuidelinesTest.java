package io.miragon.common.architecture;

import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import java.io.PrintStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * ArchUnit half of the combined suite: general coding guidelines that rely on the resolved bytecode
 * graph (package structure, freedom of cycles, no {@code println}).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BasicCodingGuidelinesTest {

    private final String pathFromRoot;

    private final JavaClasses allClasses;

    private final JavaClasses productionClasses;

    protected BasicCodingGuidelinesTest(String pathFromRoot) {
        this.pathFromRoot = pathFromRoot;
        this.allClasses =
                new ClassFileImporter()
                        .importPackages(pathFromRoot);
        this.productionClasses =
                new ClassFileImporter()
                        .withImportOption(new DoNotIncludeTests())
                        .importPackages(pathFromRoot);
    }

    public String getPathFromRoot() {
        return pathFromRoot;
    }

    @Test
    void each_class_has_package_declaration() {
        ArchRuleDefinition
                .classes()
                .should()
                .resideInAnyPackage(pathFromRoot + "..")
                .because("All classes should be in the specified package structure")
                .check(allClasses);
    }

    @Test
    void classes_are_free_of_cycles() {
        SlicesRuleDefinition
                .slices()
                .matching(pathFromRoot + ".(**)")
                .should()
                .beFreeOfCycles()
                .because("Classes should not have circular dependencies")
                .check(allClasses);
    }

    @Test
    void production_code_does_not_use_println_or_System_out() {
        // Any println overload: Kotlin's println("...") ended up in PrintStream.println(Object), but javac binds
        // System.out.println("...") to println(String) — checking println(Object) alone would miss it.
        ArchRuleDefinition
                .noClasses()
                .should()
                .callMethodWhere(target(name("println")).and(target(owner(assignableTo(PrintStream.class)))))
                .because("Use a logger instead of println or System.out for diagnostic output")
                .check(productionClasses);
    }
}
