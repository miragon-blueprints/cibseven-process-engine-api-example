import io.miragon.bpmn.adapter.GenerateBpmnModelsTask
import io.miragon.bpmn.domain.shared.OutputLanguage
import io.miragon.bpmn.domain.shared.ProcessEngine
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage
import org.springframework.boot.gradle.tasks.bundling.BootJar
import java.math.BigDecimal

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.springframework)
    alias(libs.plugins.spring.dependency)
    alias(libs.plugins.bpmnToCode)
    alias(libs.plugins.pitest)
}

springBoot {
    buildInfo()
}

configurations.all {
    // CIB seven 2.2.0 pulls in both the generic `cibseven-webclient-web` and the Spring-Boot-4
    // variant `cibseven-webclient-web-spring-boot-4`. The generic one calls
    // PathMatchConfigurer.setUseSuffixPatternMatch(...), which was removed in Spring 7 / Spring
    // Boot 4, and crashes the app on start-up. Drop it so only the SB4 variant remains.
    exclude(group = "org.cibseven.webapp", module = "cibseven-webclient-web")
}

dependencies {
    // The process-engine-adapter BOM aligns the process-engine-api / adapter / worker versions.
    implementation(platform(libs.process.engine.adapter.cib7.bom))
    implementation(libs.bundles.defaultService)
    implementation(libs.bundles.database)
    // The embedded CIB seven engine still ships the webapp (Cockpit/Tasklist) and the `/engine-rest`
    // API used by the Bruno e2e scenarios; the process-engine-api layer is added on top of it.
    implementation(libs.bundles.cibseven)
    // process-engine-api: the abstraction used to drive the engine (start/correlate/complete) and to
    // implement the BPMN service tasks as `@ProcessEngineWorker` beans consuming external tasks.
    implementation(libs.bundles.processEngineApi)
    implementation(libs.springdoc)
    implementation(libs.bpmn.to.code.runtime)
    testImplementation(libs.bundles.test)
    testImplementation(libs.bundles.cib7ProcessTest)
    testImplementation(libs.bundles.cib7JGiven)
    testImplementation(libs.bpmn.to.code.testing)
    testImplementation(project(":service:common-architecture-tests"))
}

// Generates the typed `*ProcessApi` objects (element ids, messages, timers, variables, …) from the
// BPMN models, so workers and tests reference process elements as compile-checked constants.
tasks.register<GenerateBpmnModelsTask>("generateBpmnModels") {
    baseDir = projectDir.toString()
    filePattern = "src/main/resources/bpmn/*.bpmn"
    outputFolderPath = "$projectDir/src/main/kotlin"
    packagePath = "io.miragon.blueprint.adapter.process"
    outputLanguage = OutputLanguage.KOTLIN
    processEngine = ProcessEngine.CAMUNDA_7
}

tasks.named("classes") {
    dependsOn("generateBpmnModels")
}

tasks.test {
    useJUnitPlatform()
    forkEvery = 1
}

val mutationTargetClasses = (project.findProperty("mutationTargetClasses") as String?)
    ?.split(",")?.map(String::trim)?.filter(String::isNotEmpty)

pitest {
    junit5PluginVersion.set("1.2.2")
    targetClasses.set(mutationTargetClasses ?: listOf("io.miragon.blueprint.*"))
    targetTests.set(listOf("io.miragon.blueprint.*"))
    failWhenNoMutations.set(false)
    excludedClasses.set(
        listOf(
            "io.miragon.blueprint.adapter.process.*ProcessApi*",
            "io.miragon.blueprint.adapter.process.HistoryCleanupConfiguration*",
            "io.miragon.blueprint.adapter.process.EngineApiConfiguration*",
            "io.miragon.blueprint.CibsevenBikeLeasingApplication*",
            "io.miragon.blueprint.BikeCatalogueSeeder*",
            "io.miragon.blueprint.adapter.inbound.rest.DevCorsConfiguration*",
            "io.miragon.blueprint.adapter.inbound.rest.OpenApiConfiguration*",
            // External-task worker beans are thin @ProcessEngineWorker adapters exercised by the
            // process tests, which mutation testing excludes.
            "io.miragon.blueprint.adapter.inbound.cibseven.*",
        ),
    )
    excludedTestClasses.set(
        listOf(
            "io.miragon.blueprint.process.*",
            "io.miragon.blueprint.architecture.*",
        ),
    )
    threads.set(Runtime.getRuntime().availableProcessors())
    timeoutFactor.set(BigDecimal("2.0"))
    avoidCallsTo.set(listOf("kotlin.jvm.internal", "mu", "org.slf4j", "io.github.oshai"))
    mutators.set(listOf("DEFAULTS"))
    outputFormats.set(listOf("HTML", "XML"))
    timestampedReports.set(false)
    mutationThreshold.set(80)
}

tasks.withType<BootJar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// OCI image for the backend, built by Spring's Cloud Native Buildpacks integration — no Dockerfile to
// maintain (layered, non-root by default). See docs/adr/0011 and the "Run it in containers" section of
// CONTRIBUTING.md. Build with `./gradlew :service:app:bootBuildImage`.
tasks.named<BootBuildImage>("bootBuildImage") {
    imageName.set("cibseven-process-engine-api-example/app:${project.version}")
    // Pin the JVM the buildpack installs to the version the code targets.
    environment.set(mapOf("BP_JVM_VERSION" to "21"))
}

java.sourceCompatibility = JavaVersion.VERSION_21
