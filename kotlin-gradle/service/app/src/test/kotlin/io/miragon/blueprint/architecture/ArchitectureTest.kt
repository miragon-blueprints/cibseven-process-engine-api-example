package io.miragon.blueprint.architecture

import io.miragon.common.architecture.ServiceArchitectureTest

/**
 * Runs the reusable ArchUnit + Konsist suite against this service's base package.
 * The rules live in the `:service:common-architecture-tests` module.
 */
class ArchitectureTest : ServiceArchitectureTest("io.miragon.blueprint")
