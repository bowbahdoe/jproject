package dev.mccue.jproject;

import dev.mccue.jproject.model.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
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
        // System.out.println(cmd);
        crashOn(
                new ProcessBuilder(cmd)
                        .inheritIO()
                        .start()
                        .waitFor()
        );
    }

    private static void runTool(String tool, List<String> args) throws IOException, InterruptedException {
        /*System.out.print(tool);
        System.out.print(": ");
        System.out.println(args);*/
        crashOn(
                ToolProvider.findFirst(tool)
                        .orElseThrow()
                        .run(System.out, System.err, args.toArray(new String[0]))
        );
    }

    private static void clean() throws Exception {
        FileUtils.deleteDirectory(Conventions.TARGET_DIR.toFile());
    }

    // jproject --alias=test java --class-path ,,, Main.java
    // javac Main.java Lib.java other/A.java
    // javac --class-path a:b:c src/**.java
    // javac --module-path a:b:c --module a.b.c
    // "artifacts" <- libraries
    // "classes" <- units of code
    // "packages" <- namespaces
    // "modules" <- explicit encapsulation ...
    // jproject run
    // jproject test
    // jproject eject
    private static void compile(ApplicationModule project) throws Exception {
        clean();
        var path = Basis.usingMavenCentral()
                .addDependencies(project.dependencies(AvailableDuring.NORMAL_COMPILE_TIME))
                .build()
                .path();

        var javacArgs = new ArrayList<>(List.of(
                "-g", // Generates debug symbols. Should always do this
                "-Xlint:all,-processing",
                "--module-path", path,
                "--processor-module-path", path,
                "--add-modules",
                "ALL-MODULE-PATH",
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
                                
                [bench-only-dependencies]
                """.formatted(projectName),
                StandardOpenOption.CREATE_NEW
        );

        var srcDirectory = Path.of(projectDirectory.toString(), "src");
        Files.createDirectory(srcDirectory);

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
    }
    private static void compileTest(ApplicationModule project) throws Exception {
        var path = Basis.usingMavenCentral()
                .addDependencies(project.dependencies(AvailableDuring.TEST_COMPILE_TIME))
                .addDependencies(project.dependencies(AvailableDuring.NORMAL_COMPILE_TIME))
                .build()
                .path();
        var javacArgs = new ArrayList<>(List.of(
                "-g", // Generates debug symbols. Should always do this
                "-Xlint:all,-processing",
                "--class-path",
                Basis.builder()
                        .addPath(SRC_CLASSES_DIR)
                        .build()
                        .path(),
                "--module-path",
                path,
                "--processor-module-path",
                path,
                "--add-modules",
                "ALL-MODULE-PATH",
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
        var path = Basis.usingMavenCentral()
                .addDependencies(project.dependencies(AvailableDuring.BENCH_COMPILE_TIME))
                .addDependencies(project.dependencies(AvailableDuring.NORMAL_COMPILE_TIME))
                .build()
                .path();

        var javacArgs = new ArrayList<>(List.of(
                "-g", // Generates debug symbols. Should always do this
                "-Xlint:all,-processing",
                "--module-path",
                path,
                "--processor-module-path",
                path,
                "--add-modules",
                "ALL-MODULE-PATH",
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

        if (args.length == 0) {
            System.out.println(Messages.USAGE);
        }
        else {
            var subcommand = args[0];
            if ("new".equals(subcommand)) {
                // Create a new project
                newProject();
                return;
            }
            // Try to find jproject.toml
            if (!Files.exists(Conventions.JPROJECT_TOML_PATH)) {
                System.err.println("could not find jproject.toml");
                System.exit(1);
            }

            // Load in info from said file
            var project = ApplicationModule.fromFile(
                    Conventions.JPROJECT_TOML_PATH
            );
            switch (subcommand) {


                // Formats code
                case "fmt" -> {
                    var basis = Basis.usingMavenCentral()
                            .addDependencies(GOOGLE_JAVA_FORMAT_DEPENDENCIES)
                            .build();
                    var formatCommand = new ArrayList<>(List.of(
                            "java",
                            "--module-path",
                            basis.path(),
                            "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
                            "--add-exports", "jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
                            "--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
                            "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
                            "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
                            "--add-modules", "ALL-MODULE-PATH",
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
                    runCommand(List.of(
                            "java",
                            "--class-path",
                            Basis.builder()
                                    .addPath(SRC_CLASSES_DIR)
                                    .build()
                                    .path(),
                            "--module-path",
                            Basis.usingMavenCentral()
                                    .addDependencies(project.dependencies(AvailableDuring.NORMAL_RUN_TIME))
                                    .build()
                                    .path(),

                            "--add-modules",
                            "ALL-MODULE-PATH",
                            project.mainClass()
                    ));
                }

                // Run tests with junit
                case "test" -> {
                    compile(project);
                    compileTest(project);

                    var jacocoBasis = Basis.usingMavenCentral()
                            .addDependency(new MavenDependency("org.jacoco", "org.jacoco.agent", "0.8.7"))
                            .build();
                    runCommand(List.of(
                            "java",
                            "-jar",
                            JUNIT_RUNNER_PATH.toString(),
                            "--class-path",
                            Basis.usingMavenCentral()
                                    .addDependencies(project.dependencies(AvailableDuring.TEST_RUN_TIME))
                                    .addDependencies(project.dependencies(AvailableDuring.NORMAL_RUN_TIME))
                                    .addPath(SRC_CLASSES_DIR)
                                    .addPath(TEST_CLASSES_DIR)
                                    .build()
                                    .path(),
                            "--scan-classpath"
                    ));
                }
                // Generate docs with javadoc
                case "doc" -> {
                    var javadocArgs = new ArrayList<>( List.of(
                            "-d",
                            "target/doc",
                            "--module-path", Basis.usingMavenCentral()
                                    .addDependencies(project.dependencies(AvailableDuring.NORMAL_COMPILE_TIME))
                                    .build()
                                    .path(),
                            "--add-modules",
                            "ALL-MODULE-PATH",
                            "--source-path", "src",
                            "--show-packages", "all"
                    ));
                    FileUtils
                            .listFiles(SRC_DIR.toFile(), new String[] { "java" }, true)
                            .forEach(sourceFile -> javadocArgs.add(sourceFile.toString()));
                    runTool("javadoc", javadocArgs);
                }

                // Run benchmarks with JMH
                case "bench" -> {
                    compile(project);
                    compileBench(project);
                    runCommand(List.of(
                            "java",
                            "--class-path",
                            Basis.usingMavenCentral()
                                    .addDependencies(project.dependencies(AvailableDuring.BENCH_RUN_TIME))
                                    .addDependencies(project.dependencies(AvailableDuring.NORMAL_RUN_TIME))
                                    .addPath(SRC_CLASSES_DIR)
                                    .addPath(BENCH_CLASSES_DIR)
                                    .build()
                                    .path(),
                            "org.openjdk.jmh.Main"
                    ));
                }
                // Print out the tree of dependencies
                case "tree" -> {
                    // TODO: test, compile, bench, etc
                    Basis.usingMavenCentral()
                            .addDependencies(project.dependencies(AvailableDuring.NORMAL_COMPILE_TIME))
                            .build()
                            .printTree();
                }

            }
        }

    }
}
