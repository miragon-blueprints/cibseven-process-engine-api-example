package io.miragon.blueprint.process.util

import io.miragon.bpmn.runtime.path.ProcessPath
import org.cibseven.bpm.engine.test.assertions.bpmn.ProcessInstanceAssert

fun ProcessInstanceAssert.hasPassedInOrder(sequentialPath: ProcessPath<*>): ProcessInstanceAssert =
    hasPassedInOrder(*sequentialPath.ids)

fun ProcessInstanceAssert.hasPassed(path: ProcessPath<*>): ProcessInstanceAssert =
    hasPassed(*path.distinctIds)
