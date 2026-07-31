package io.miragon.common.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Konsist half of the combined suite: the *source-structure* rules that ArchUnit cannot see, because
 * the Kotlin compiler erases them into synthetic bytecode.
 *
 * The generated `adapter.process` sources are excluded — they are machine-written from the BPMN
 * models by the bpmn-to-code plugin and are not hand-maintained code.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class KotlinSourceGuidelinesTest(
    val pathFromRoot: String,
) {

    @Test
    fun `no wildcard imports`() {
        Konsist
            .scopeFromProject()
            .files
            .filterNot { it.hasPackage("..adapter.process..") }
            .flatMap { it.imports }
            .assertFalse {
                it.isWildcard && !it.name.startsWith("java.util")
            }
    }

    /**
     * ArchUnit reads bytecode, where the Kotlin compiler merges every top-level declaration of a
     * `.kt` file into synthetic class files — so it cannot tell how many declarations a source file
     * holds. Konsist reads the source directly and can. This rule is the reason both tools earn their
     * place in the combined suite.
     */
    @Test
    fun `files define at most one top-level class, interface or object`() {
        Konsist
            .scopeFromPackage("$pathFromRoot..")
            .files
            .filterNot { it.hasPackage("..adapter.process..") }
            .assertTrue { file ->
                file.classesAndInterfacesAndObjects(includeNested = false, includeLocal = false).size <= 1
            }
    }
}
