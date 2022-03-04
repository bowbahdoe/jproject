package dev.mccue.jproject;

import dev.mccue.jproject.model.ApplicationModule;
import dev.mccue.jproject.model.Basis;
import dev.mccue.jproject.model.MavenDependency;
import dev.mccue.jproject.model.MavenRepository;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;

import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.spi.ToolProvider;

import static dev.mccue.jproject.Conventions.*;

public final class Main {

    private Main() {}

    private static void crashOn(int status) {
        if (status != 0) {
            System.exit(status);
        }
    }

    private static void runCommand(List<String> cmd) throws Exception {
        System.out.println(cmd);
        crashOn(
                new ProcessBuilder(cmd)
                        .inheritIO()
                        .start()
                        .waitFor()
        );
    }

    private static void runTool(String tool, List<String> args) throws IOException, InterruptedException {
        System.out.print(tool);
        System.out.print(": ");
        System.out.println(args);
        crashOn(
                ToolProvider.findFirst(tool)
                        .orElseThrow()
                        .run(System.out, System.err, args.toArray(new String[0]))
        );
    }

    private static void clean() throws Exception {
        FileUtils.deleteDirectory(Conventions.TARGET_DIR.toFile());
    }

    private static void compile(ApplicationModule project) throws Exception {
        clean();
        var javacArgs = new ArrayList<>(List.of(
                "-g", // Generates debug symbols. Should always do this
                "-Xlint:all",
                "--module-path", Basis.usingMavenCentral()
                        .addDependencies(project.compileDependencies())
                        .build()
                        .path(),
                "-d", SRC_CLASSES_DIR.toString()
        ));

        FileUtils
                .listFiles(SRC_DIR.toFile(), new String[] { "java" }, true)
                .forEach(sourceFile -> javacArgs.add(sourceFile.toString()));

        runTool(
                "javac",
                javacArgs
        );

        FileUtils.copyDirectory(
                SRC_DIR.toFile(),
                SRC_CLASSES_DIR.toFile(),
                FileFileFilter.INSTANCE.and(new SuffixFileFilter(".java").negate())
        );

        JAR_DIR.toFile().mkdirs();
        runTool(
                "jar",
                List.of(
                        "--create",
                        "--file",
                        NORMAL_JAR_FILE.toString(),
                        "--main-class",
                        project.mainClass(),
                        "-C",
                        SRC_CLASSES_DIR.toString(),
                        "."
                )
        );
    }

    private static void newProject() throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.print("primary module name: ");
        var projectName = scanner.next();

        var projectDirectory = Path.of(projectName);
        try {
            Files.createDirectory(projectDirectory);
        } catch (FileAlreadyExistsException __) {
            System.err.println("directory already exists");
            System.exit(1);
        }

        Files.writeString(Path.of(projectDirectory.toString(), "jproject.toml"), """
                [application]
                main-class = "%s.Main"
                                
                [dependencies]
                                
                [test-only-dependencies]
                "org.junit.jupiter/junit-jupiter-api" = "5.8.2"
                                
                [runtime-only-dependencies]
                                
                [compile-only-dependencies]
                """.formatted(projectName),
                StandardOpenOption.CREATE_NEW
        );

        var srcDirectory = Path.of(projectDirectory.toString(), "src");
        Files.createDirectory(srcDirectory);
        Files.writeString(Path.of(srcDirectory.toString(), "module-info.java"), """
                module %s {
                    requires org.junit.jupiter.api;
                }
                """.formatted(projectName),
                StandardOpenOption.CREATE_NEW
        );

        var paths = projectName.split("\\.");
        var main = Path.of(Path.of(srcDirectory.toString(), paths).toString(), "Main.java");
        main.getParent().toFile().mkdirs();
        Files.writeString(
                main, """
                package %s;
                
                public final class Main {
                    private Main() {}
                    
                    public static void main(String[] args) {
                        System.out.println("Hello, world");
                    }
                }
                """.formatted(projectName),
                StandardOpenOption.CREATE_NEW
        );

        var testDirectory = Path.of(projectDirectory.toString(), "test");
        Files.createDirectory(testDirectory);
        Files.writeString(Path.of(testDirectory.toString(), "module-info.java"), """
                module %s {
                    requires org.junit.jupiter.api;
                }
                """.formatted(projectName),
                StandardOpenOption.CREATE_NEW
        );


        var test = Path.of(Path.of(testDirectory.toString(), paths).toString(), "BasicTest.java");
        test.getParent().toFile().mkdirs();
        Files.writeString(test, """
                package %s;
                
                import org.junit.jupiter.api.Test;
                
                import static org.junit.jupiter.api.Assertions.assertEquals;
                
                public final class BasicTest {
                    @Test
                    public void basicTest() {
                        assertEquals(1 + 1, 2);
                    }
                }
                """.formatted(projectName),
                StandardOpenOption.CREATE_NEW
        );

        var benchDirectory = Path.of(projectDirectory.toString(), "bench");
        Files.createDirectory(benchDirectory);
        Files.writeString(Path.of(benchDirectory.toString(), "module-info.java"), """
                module %s {
                    
                }
                """.formatted(projectName),
                StandardOpenOption.CREATE_NEW
        );
    }
    private static void compileTest(ApplicationModule project) throws Exception {
        var javacArgs = new ArrayList<>(List.of(
                "-g", // Generates debug symbols. Should always do this
                "-Xlint:all",
                "--module-path",
                Basis.usingMavenCentral()
                        .addDependencies(project.testCompileDependencies())
                        .build()
                        .path(),
                "-d", Conventions.TEST_CLASSES_DIR.toString()
        ));

        FileUtils
                .listFiles(Conventions.TEST_DIR.toFile(), new String[] { "java" }, true)
                .forEach(sourceFile -> javacArgs.add(sourceFile.toString()));

        runTool(
                "javac",
                javacArgs
        );
    }

    private static void compileBench(ApplicationModule project) throws Exception {
        var javacArgs = new ArrayList<>(List.of(
                "-g", // Generates debug symbols. Should always do this
                "-Xlint:all",
                "--module-path",
                Basis.usingMavenCentral()

                        .addDependency(
                                new MavenDependency(
                                        new MavenDependency.Coordinate("org.openjdk.jmh", "jmh-core"),
                                        "1.21",
                                        List.of()
                                )
                        )
                        .addDependency(
                                new MavenDependency(
                                        new MavenDependency.Coordinate("org.openjdk.jmh", "jmh-generator-annprocess"),
                                        "1.21",
                                        List.of()
                                )
                        )
                        .addDependencies(project.compileDependencies())
                        .build()
                        .path(),
                "-d", BENCH_CLASSES_DIR.toString()
        ));

        FileUtils
                .listFiles(BENCH_DIR.toFile(), new String[] { "java" }, true)
                .forEach(sourceFile -> javacArgs.add(sourceFile.toString()));

        runTool(
                "javac",
                javacArgs
        );
    }
    public static void main(String[] args) throws Exception {
        Setup.setUp();
        // Try to find jproject.toml
        if (!Files.exists(Conventions.JPROJECT_TOML_PATH)) {
            System.err.println("could not find jproject.toml");
            System.exit(1);
        }

        // Load in info from said file
        var project = ApplicationModule.fromFile(
                Conventions.JPROJECT_TOML_PATH
        );

        System.out.println(ModuleFinder.of(Arrays.stream(Basis.usingMavenCentral()

                .addDependencies(GOOGLE_JAVA_FORMAT_DEPENDENCIES)
                .build()
                .path()
                .split(":"))
                        .map(Path::of)
                .toArray(Path[]::new)).findAll());

        if (args.length == 0) {
            System.out.println(Messages.USAGE);
        }
        else {
            var subcommand = args[0];
            switch (subcommand) {
                // Create a new project
                case "new" -> {
                    newProject();
                }

                // Formats code
                case "fmt" -> {
                    var basis = Basis.usingMavenCentral()
                            .addDependencies(GOOGLE_JAVA_FORMAT_DEPENDENCIES)
                            .build();
                    var formatCommand = new ArrayList<>(List.of(
                            "java",
                            "--class-path",
                            basis.path(),
                            "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
                            "--add-exports", "jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
                            "--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
                            "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
                            "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
                            "com.google.googlejavaformat.java.Main",
                            "--aosp",
                            "-r"
                    ));
                    FileUtils.listFiles(SRC_DIR.toFile(), new String[] { "java" }, true).forEach(file ->
                            formatCommand.add(file.toString())
                    );
                    FileUtils.listFiles(TEST_DIR.toFile(), new String[] { "java" }, true).forEach(file ->
                            formatCommand.add(file.toString())
                    );
                    FileUtils.listFiles(BENCH_DIR.toFile(), new String[] { "java" }, true).forEach(file ->
                            formatCommand.add(file.toString())
                    );
                    runCommand(formatCommand);
                }

                // Clean all cached resources
                case "clean" -> {
                    clean();
                }
                // Compile all modules
                case "compile" -> {
                    compile(project);
                }
                // Run the project

                case "run" -> {
                    compile(project);

                    var moduleName = List.copyOf(ModuleFinder.of(Conventions.JAR_DIR).findAll())
                            .get(0)
                            .descriptor()
                            .name();

                    runCommand(List.of(
                            "java",
                            "--module-path",
                            Basis.usingMavenCentral()
                                    .addPath(Path.of(JAR_DIR.toString(), "app.jar"))
                                    .addDependencies(project.runtimeDependencies())
                                    .build()
                                    .path(),
                            "--module",
                            moduleName
                    ));
                }

                // Run tests with junit
                case "test" -> {
                    compile(project);
                    compileTest(project);
                    var moduleName = List.copyOf(ModuleFinder.of(Conventions.JAR_DIR).findAll())
                            .get(0)
                            .descriptor()
                            .name();

                    runCommand(List.of(
                            "java",
                            "--add-reads", moduleName + "=ALL-UNNAMED",
                            "--add-opens", moduleName + "/" + moduleName + "=ALL-UNNAMED",
                            "--module-path",
                            Basis.usingMavenCentral()
                                    .addPath(Path.of(JAR_DIR.toString(), "app.jar"))
                                    .addPath(Conventions.TEST_CLASSES_DIR)
                                    .addDependencies(project.testRuntimeDependencies())
                                    .build()
                                    .path(),
                            "-jar",
                            JUNIT_RUNNER_PATH.toString(),
                            "--scan-modules"
                    ));
                    System.out.println("test");
                }
                // Generate docs with javadoc
                case "doc" -> {
                    runTool(
                            "javadoc",
                            List.of(
                                "-d",
                                "target/doc",
                                "--module-path", Basis.usingMavenCentral()
                                        .addDependencies(project.compileDependencies())
                                        .build()
                                        .path(),
                                "--source-path", "src",
                                "dev.mccue.example"
                            )
                    );
                }

                // Run benchmarks with JMH
                case "bench" -> {
                    compile(project);
                    compileBench(project);
                    System.exit(1);
                }
                // Print out the tree of dependencies
                case "tree" -> {
                    // TODO: test, compile, bench, etc
                    Basis.usingMavenCentral()
                            .addDependencies(project.runtimeDependencies())
                            .build()
                            .printTree();
                }

            }
        }

    }
}
