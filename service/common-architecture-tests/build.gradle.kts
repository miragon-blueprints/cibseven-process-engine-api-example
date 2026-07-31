import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.springframework)
    alias(libs.plugins.spring.dependency)
}

/**
 * Reusable architecture-test suite (ArchUnit + Konsist), consumed by services via
 * `testImplementation(project(":service:common-architecture-tests"))`.
 *
 * Rules live in `src/main` so they are published on the consumer's classpath. `api` exposes the
 * ArchUnit / Konsist / JUnit dependencies transitively to the consuming service's test classpath.
 */
dependencies {
    api(libs.bundles.test)
    api(libs.bundles.archunit)
    api(libs.bundles.konsist)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

tasks.getByName<BootJar>("bootJar") {
    enabled = false
}
