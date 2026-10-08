package io.miragon.common.architecture;

import org.junit.jupiter.api.Nested;

/**
 * A single, ready-to-use architecture suite for a service — combining <b>ArchUnit</b> and <b>JavaParser</b>,
 * each doing what it is best at.
 *
 * <p>A service wires up the full suite with one small subclass:
 *
 * <pre>{@code
 * class ArchitectureTest extends ServiceArchitectureTest {
 *     ArchitectureTest() {
 *         super("io.miragon.blueprint");
 *     }
 * }
 * }</pre>
 *
 * <h2>Why a mix, and who owns what</h2>
 *
 * <ul>
 *   <li><b>ArchUnit</b> reads compiled <b>bytecode</b>, so it sees the fully resolved dependency graph. It owns
 *   the <em>dependency &amp; structure</em> rules — hexagonal layering, technology-neutrality of domain &amp;
 *   application, port/adapter isolation ({@link Dependencies}), naming conventions ({@link Naming}), freedom of
 *   cycles and the no-{@code println} check ({@link CodingGuidelines}).</li>
 *   <li><b>JavaParser</b> reads Java <b>source</b>, so it sees what never makes it into the bytecode. It owns
 *   the <em>source-structure</em> rules — one top-level type per file (SRP) and no wildcard imports
 *   ({@link JavaSource}).</li>
 * </ul>
 *
 * <p>This module is <b>self-contained</b>: it carries both the ArchUnit and JavaParser dependencies and its
 * own copies of the rules, so it can be dropped into a service as a single test dependency.
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

    @Nested
    public final class JavaSource extends JavaSourceGuidelinesTest {

        JavaSource() {
            super(ServiceArchitectureTest.this.rootPackage);
        }
    }
}
