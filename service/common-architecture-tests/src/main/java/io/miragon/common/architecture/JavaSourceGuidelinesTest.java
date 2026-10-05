package io.miragon.common.architecture;

import static org.junit.jupiter.api.Assertions.fail;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.tngtech.archunit.core.domain.PackageMatcher;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * JavaParser half of the combined suite: the <em>source-structure</em> rules that ArchUnit cannot see, because
 * the Java compiler does not carry them into the bytecode.
 *
 * <p>The sources are read from the project root — the nearest directory at or above {@code user.dir} that
 * contains {@code .mvn/} — skipping hidden directories and build output ({@code target}, {@code build},
 * {@code node_modules}), and parsed with the Java 21 language level.
 *
 * <p>The generated {@code adapter.process} sources are excluded — they are machine-written from the BPMN
 * models by the bpmn-to-code plugin and are not hand-maintained code.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class JavaSourceGuidelinesTest {

    /** Generated sources, excluded from both rules. */
    private static final PackageMatcher GENERATED_PROCESS_API = PackageMatcher.of("..adapter.process..");

    /** Wildcard imports are still allowed for names starting with this prefix. */
    private static final String ALLOWED_WILDCARD_IMPORT_PREFIX = "java.util";

    /** Directories below the project root that hold no hand-written sources. */
    private static final Set<String> SKIPPED_DIRECTORIES = Set.of("target", "build", "node_modules");

    private final String pathFromRoot;

    protected JavaSourceGuidelinesTest(String pathFromRoot) {
        this.pathFromRoot = pathFromRoot;
    }

    public String getPathFromRoot() {
        return pathFromRoot;
    }

    /** Checks every Java source file of the project (all modules, main and test), not only {@code pathFromRoot}. */
    @Test
    void no_wildcard_imports() {
        List<ParsedSource> sources =
                parseProjectSources()
                        .stream()
                        .filter(source -> !GENERATED_PROCESS_API.matches(source.packageName()))
                        .toList();
        int checkedImports = 0;
        List<String> violations = new ArrayList<>();
        for (ParsedSource source : sources) {
            for (ImportDeclaration declaration : source.unit().getImports()) {
                String name = declaration.getNameAsString();
                checkedImports++;
                if (declaration.isAsterisk() && !name.startsWith(ALLOWED_WILDCARD_IMPORT_PREFIX)) {
                    String staticModifier = declaration.isStatic() ? "static " : "";
                    violations.add(source.path() + ": import " + staticModifier + name + ".*");
                }
            }
        }

        if (checkedImports == 0) {
            fail("No imports found in the project's Java sources — wrong project root?");
        }
        if (!violations.isEmpty()) {
            fail("Wildcard imports are not allowed (except " + ALLOWED_WILDCARD_IMPORT_PREFIX + "), found "
                    + violations.size() + ":\n  " + String.join("\n  ", violations));
        }
    }

    /**
     * ArchUnit reads bytecode, where every top-level type becomes a class file of its own and the only trace of
     * the source file is the optional {@code SourceFile} debug attribute — so it cannot reliably tell how many
     * types a source file holds. JavaParser reads the source directly and can. This rule is the reason both tools
     * earn their place in the combined suite.
     *
     * <p>A top-level type is a class, interface, enum, record or annotation; nested and local types do not count.
     * Like the import rule it covers main and test sources of all modules, here restricted to files whose package
     * lies in {@code pathFromRoot}.
     */
    @Test
    void files_define_at_most_one_top_level_type() {
        PackageMatcher scope = PackageMatcher.of(pathFromRoot + "..");
        List<ParsedSource> sources =
                parseProjectSources()
                        .stream()
                        .filter(source -> scope.matches(source.packageName()))
                        .filter(source -> !GENERATED_PROCESS_API.matches(source.packageName()))
                        .toList();
        List<String> violations =
                sources.stream()
                        .filter(source -> source.unit().getTypes().size() > 1)
                        .map(source -> source.path() + " (" + source.unit().getTypes().stream()
                                .map(TypeDeclaration::getNameAsString)
                                .collect(Collectors.joining(", ")) + ")")
                        .toList();

        if (sources.isEmpty()) {
            fail("No Java sources found in package " + pathFromRoot + " — wrong project root?");
        }
        if (!violations.isEmpty()) {
            fail("Files must define at most one top-level type, found " + violations.size() + ":\n  "
                    + String.join("\n  ", violations));
        }
    }

    private static List<ParsedSource> parseProjectSources() {
        Path projectRoot = projectRoot();
        JavaParser parser = new JavaParser(new ParserConfiguration().setLanguageLevel(LanguageLevel.JAVA_21));
        List<ParsedSource> parsed = new ArrayList<>();
        List<String> unparseable = new ArrayList<>();
        for (Path file : javaSourceFiles(projectRoot)) {
            Path relativePath = projectRoot.relativize(file);
            ParseResult<CompilationUnit> result;
            try {
                result = parser.parse(file);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not read " + relativePath, e);
            }
            if (result.isSuccessful() && result.getResult().isPresent()) {
                parsed.add(new ParsedSource(relativePath, result.getResult().get()));
            } else {
                unparseable.add(relativePath + ": " + result.getProblems().stream()
                        .map(Problem::getVerboseMessage)
                        .collect(Collectors.joining("; ")));
            }
        }
        if (!unparseable.isEmpty()) {
            throw new IllegalStateException(
                    "Could not parse " + unparseable.size() + " Java source file(s) as Java 21:\n  "
                            + String.join("\n  ", unparseable));
        }
        return parsed;
    }

    /** The nearest directory at or above {@code user.dir} that contains {@code .mvn/}. */
    private static Path projectRoot() {
        Path workingDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        for (Path directory = workingDirectory; directory != null; directory = directory.getParent()) {
            if (Files.isDirectory(directory.resolve(".mvn"))) {
                return directory;
            }
        }
        throw new IllegalStateException(
                "No project root (a directory containing .mvn/) found at or above " + workingDirectory);
    }

    /** All {@code *.java} files below the project root, sorted, without hidden and build-output directories. */
    private static List<Path> javaSourceFiles(Path projectRoot) {
        List<Path> files = new ArrayList<>();
        try {
            Files.walkFileTree(projectRoot, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) {
                    if (directory.equals(projectRoot)) {
                        return FileVisitResult.CONTINUE;
                    }
                    String name = directory.getFileName().toString();
                    boolean skipped = name.startsWith(".") || SKIPPED_DIRECTORIES.contains(name);
                    return skipped ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                    if (file.getFileName().toString().endsWith(".java")) {
                        files.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Could not scan " + projectRoot + " for Java sources", e);
        }
        files.sort(Comparator.naturalOrder());
        return files;
    }

    /** A parsed source file together with its path relative to the project root. */
    private record ParsedSource(Path path, CompilationUnit unit) {

        String packageName() {
            return unit.getPackageDeclaration().map(PackageDeclaration::getNameAsString).orElse("");
        }
    }
}
