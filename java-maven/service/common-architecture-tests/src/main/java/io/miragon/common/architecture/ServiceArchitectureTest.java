package io.miragon.common.architecture;

import org.junit.jupiter.api.Nested;

/**
 * A single, ready-to-use ArchUnit architecture suite for a service. A service wires up the full suite
 * with one small subclass:
 *
 * <pre>{@code
 * class ArchitectureTest extends ServiceArchitectureTest {
 *     ArchitectureTest() {
 *         super("io.miragon.blueprint");
 *     }
 * }
 * }</pre>
 *
 * <p>ArchUnit reads compiled <b>bytecode</b>, so it sees the fully resolved dependency graph. It owns the
 * <em>dependency &amp; structure</em> rules — hexagonal layering, technology-neutrality of domain &amp;
 * application, port/adapter isolation ({@link Dependencies}), naming conventions ({@link Naming}), freedom of
 * cycles and the no-{@code println} check ({@link CodingGuidelines}). The two <em>source-structure</em> rules
 * bytecode cannot express (no wildcard imports, one top-level type per file) are enforced by Checkstyle in
 * the Maven build.
 *
 * <p>This module is <b>self-contained</b>: it carries the ArchUnit dependency and its own copies of the
 * rules, so it can be dropped into a service as a single test dependency.
 */
public abstract class ServiceArchitectureTest {

    private final String rootPackage;

    protected ServiceArchitectureTest(String rootPackage) {
        this.rootPackage = rootPackage;
    }

    @Nested
    public final class Dependencies extends HexagonalArchitectureTest {

        Dependencies() {
            super(ServiceArchitectureTest.this.rootPackage);
        }
    }

    @Nested
    public final class Naming extends NamingConventionArchitectureTest {

        Naming() {
            super(ServiceArchitectureTest.this.rootPackage);
        }
    }

    @Nested
    public final class CodingGuidelines extends BasicCodingGuidelinesTest {

        CodingGuidelines() {
            super(ServiceArchitectureTest.this.rootPackage);
        }
    }
}
