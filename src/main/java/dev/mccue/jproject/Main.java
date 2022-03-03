package dev.mccue.jproject;

import dev.mccue.jproject.model.ApplicationModule;
import dev.mccue.jproject.model.Basis;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;

import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
                "--module-path", Basis.builder()
                        .addDependencies(project.compileDependencies())
                        .build()
                        .path(),
                "-d", Conventions.CLASSES_DIR.toString()
        ));

        FileUtils
                .listFiles(Conventions.SRC_DIR.toFile(), new String[] { "java" }, true)
                .forEach(sourceFile -> javacArgs.add(sourceFile.toString()));

        runTool(
                "javac",
                javacArgs
        );

        FileUtils.copyDirectory(
                Conventions.SRC_DIR.toFile(),
                Conventions.CLASSES_DIR.toFile(),
                FileFileFilter.INSTANCE.and(new SuffixFileFilter(".java").negate())
        );

        Conventions.JAR_DIR.toFile().mkdirs();
        runTool(
                "jar",
                List.of(
                        "--create",
                        "--file",
                        Path.of(Conventions.JAR_DIR.toString(), "app.jar").toString(),
                        "--main-class",
                        project.mainClass(),
                        "-C",
                        Conventions.CLASSES_DIR.toString(),
                        "."
                )
        );
    }

    private static void compileTest(ApplicationModule project) throws Exception {
        var javacArgs = new ArrayList<>(List.of(
                "-g", // Generates debug symbols. Should always do this
                "-Xlint:all",
                "--module-path", Basis.builder()
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

        if (args.length == 0) {
            System.out.println(Messages.USAGE);
        }
        else {
            var subcommand = args[0];
            switch (subcommand) {
                // Create a new project
                case "new" -> {}

                // Formats code
                case "fmt" -> {
                    var formatCommand = new ArrayList<>(List.of(
                            "java",
                            "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
                            "--add-exports", "jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
                            "--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
                            "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
                            "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
                            "-jar",
                            GOOGLE_JAVA_FORMAT_PATH.toString(),
                            "--aosp",
                            "-r"
                    ));
                    FileUtils.listFiles(SRC_DIR.toFile(), new String[] { "java" }, true).forEach(file ->
                            formatCommand.add(file.toString())
                    );
                    FileUtils.listFiles(TEST_DIR.toFile(), new String[] { "java" }, true).forEach(file ->
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
                            Basis.builder()
                                    .addPath(Conventions.JAR_DIR)
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
                            "--patch-module", moduleName + "=" + Conventions.TEST_CLASSES_DIR,
                            "--add-reads", moduleName + "=ALL-UNNAMED",
                            "--add-opens", moduleName + "/" + moduleName + "=ALL-UNNAMED",
                            "--module-path",
                            Basis.builder()
                                    .addPath(Conventions.JAR_DIR)
                                    .addPath(Conventions.TEST_CLASSES_DIR)
                                    .addDependencies(project.testRuntimeDependencies())
                                    .build()
                                    .path(),
                            "-jar",
                            JUNIT_RUNNER_PATH.toString(),
                            "--select-modules"
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
                                "--module-path", Basis.builder()
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
                    System.err.println("bench: NOT YET IMPLEMENTED");
                    System.exit(1);
                }
                // Publish a library to maven central
                case "publish" -> {
                    System.err.println("publish: NOT YET IMPLEMENTED");
                    System.exit(1);
                }
                // Print out the tree of dependencies
                case "tree" -> {}

            }
        }

    }
}
