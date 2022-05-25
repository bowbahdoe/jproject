package dev.mccue.jproject;

import dev.mccue.jproject.model.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.spi.ToolProvider;

import static dev.mccue.jproject.Conventions.*;

public final class Main {

    private Main() {}

    /**
     * Small helper to pull the path out of a basis, but cache results.
     */
    private static String path(Basis basis) {
        var cachePath = Path.of(".path-cache");
        Map<?, ?> cacheMap = null;
        try (var fis = new FileInputStream(cachePath.toFile());
             var ois = new ObjectInputStream(fis)) {
            cacheMap = (Map<?, ?>) ois.readObject();
            // System.err.println("Loaded cache: " + cacheMap);
        } catch (FileNotFoundException e) {
            // System.err.println("No cache file found");
            // noop
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        if (cacheMap != null &&  cacheMap.get(basis) instanceof String path) {
            // System.err.println("Using cached value: " + path);
            return path;
        }
        else {
            // System.err.println("Caching path: " + basis.path());
            var path= basis.path();
            var newCacheMap = cacheMap == null ?
                    new HashMap<>() :
                    new HashMap<Object, Object>(cacheMap);
            newCacheMap.put(basis, path);
            try (var fos = new FileOutputStream(cachePath.toFile());
                 var oos = new ObjectOutputStream(fos)) {
                oos.writeObject(newCacheMap);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return path;
        }

    }

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

    private static void compile(ApplicationModule project) throws Exception {
        clean();
        var path = path(Basis.usingMavenCentral()
                .addDependencies(project.dependencies(AvailableDuring.NORMAL_COMPILE_TIME))
                .build());

        var javacArgs = new ArrayList<>(List.of(
                "-g", // Generates debug symbols. Should always do this
                "-Xlint:all,-processing",
                "--add-modules",
                "ALL-MODULE-PATH",
                "-d", SRC_CLASSES_DIR.toString(),
                "-s", SRC_GENERATED_SOURCES_DIR.toString()
        ));

        if (!"".equals(path)) {
            javacArgs.addAll(List.of(
                    "--module-path", path,
                    "--processor-module-path", path
            ));
        }

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

    private static void newProject(String projectName) throws Exception {
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
                        "org.openjdk.jmh/jmh-core" = "1.21"
                        "org.openjdk.jmh/jmh-generator-annprocess" = { version = "1.21", available-at = ["compile-time"] }
                        """.formatted(projectName),
                StandardOpenOption.CREATE_NEW
        );

        Files.writeString(Path.of(projectDirectory.toString(), ".gitignore"), """
                        target/
                        .path-cache
                        .DS_Store
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

        /* var bench = Path.of(Path.of(benchDirectory.toString(), paths).toString(), "BasicBenchmark.java");
        bench.getParent().toFile().mkdirs();
        Files.writeString(bench, """
                        package %s;
                                        
                        import org.openjdk.jmh.annotations.Benchmark;
                                    
                        public class BasicBenchmark {
                            @Benchmark
                            public void testMethod() {
                                int a = 1;
                                int b = 2;
                                int sum = a + b;
                            }
                        }
                        """.formatted(projectName),
                StandardOpenOption.CREATE_NEW
        ); */


        new ProcessBuilder("git", "init")
                .directory(projectDirectory.toFile())
                .start();
    }
    private static void compileTest(ApplicationModule project) throws Exception {
        var path = path(Basis.usingMavenCentral()
                .addDependencies(project.dependencies(AvailableDuring.TEST_COMPILE_TIME))
                .build());
        var javacArgs = new ArrayList<>(List.of(
                "-g", // Generates debug symbols. Should always do this
                "-Xlint:all,-processing",
                "--class-path",
                path(Basis.builder()
                        .addPath(SRC_CLASSES_DIR)
                        .build()),
                "--module-path",
                path,
                "--processor-module-path",
                path,
                "--add-modules",
                "ALL-MODULE-PATH",
                "-d", Conventions.TEST_CLASSES_DIR.toString(),
                "-s", TEST_GENERATED_SOURCES_DIR.toString()
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
        var path = path(Basis.usingMavenCentral()
                .addDependencies(project.dependencies(AvailableDuring.BENCH_COMPILE_TIME))
                .build());

        var javacArgs = new ArrayList<>(List.of(
                "-g", // Generates debug symbols. Should always do this
                "-Xlint:all,-processing",
                "--module-path",
                path,
                "--processor-module-path",
                path,
                "--add-modules",
                "ALL-MODULE-PATH",
                "-d", BENCH_CLASSES_DIR.toString(),
                "-s", BENCH_GENERATED_SOURCES_DIR.toString()
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
                newProject(args[1]);
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
                            path(basis),
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
                    var runArgs = new ArrayList<>(List.of(
                            "java",
                            "--class-path",
                            path(Basis.builder()
                                    .addPath(SRC_CLASSES_DIR)
                                    .build()),
                            "--add-modules",
                            "ALL-MODULE-PATH"
                    ));

                    var deps = path(Basis.usingMavenCentral()
                            .addDependencies(project.dependencies(AvailableDuring.NORMAL_RUN_TIME))
                            .build());

                    if (!"".equals(deps)) {
                        runArgs.add("--module-path");
                        runArgs.add(deps);
                    }

                    runArgs.add(project.mainClass());

                    runCommand(runArgs);
                }

                // Run tests with junit
                case "test" -> {
                    compile(project);
                    compileTest(project);
                    runCommand(List.of(
                            "java",
                            "-jar",
                            JUNIT_RUNNER_PATH.toString(),
                            "--class-path",
                            path(Basis.usingMavenCentral()
                                    .addDependencies(project.dependencies(AvailableDuring.TEST_RUN_TIME))
                                    .addPath(SRC_CLASSES_DIR)
                                    .addPath(TEST_CLASSES_DIR)
                                    .build()),
                            "--scan-classpath"
                    ));
                }
                // Generate docs with javadoc
                case "doc" -> {
                    var javadocArgs = new ArrayList<>( List.of(
                            "-d",
                            "target/doc",
                            "--add-modules",
                            "ALL-MODULE-PATH",
                            "--source-path", "src",
                            "--show-packages", "all"
                    ));
                    var path = path(Basis.usingMavenCentral()
                            .addDependencies(project.dependencies(AvailableDuring.NORMAL_COMPILE_TIME))
                            .build());
                    if (!"".equals(path)) {
                        javadocArgs.add("--module-path");
                        javadocArgs.add(path);
                    }

                    FileUtils
                            .listFiles(SRC_DIR.toFile(), new String[] { "java" }, true)
                            .forEach(sourceFile -> javadocArgs.add(sourceFile.toString()));
                    runTool("javadoc", javadocArgs);
                }

                // Run benchmarks with JMH
                case "bench" -> {
                    compile(project);
                    compileBench(project);
                    var benchCmd = new ArrayList<>(List.of(
                            "java",
                            path(Basis.usingMavenCentral()
                                    .addPath(SRC_CLASSES_DIR)
                                    .addPath(BENCH_CLASSES_DIR)
                                    .build()),
                            "--class-path",
                            path(Basis.usingMavenCentral()
                                    .addPath(SRC_CLASSES_DIR)
                                    .addPath(BENCH_CLASSES_DIR)
                                    .build()),
                            "org.openjdk.jmh.Main"
                    ));
                    for (int i = 1; i < args.length; i++) {
                        benchCmd.add(args[i]);
                    }
                    runCommand(benchCmd);
                }
                // Print out the tree of dependencies
                case "tree" -> {
                    if (args.length == 2) {
                        if (args[1].equals("test-compile-time")) {
                            Basis.usingMavenCentral()
                                    .addDependencies(project.dependencies(AvailableDuring.TEST_COMPILE_TIME))
                                    .build()
                                    .printTree();
                        }
                        else if (args[1].equals("bench-compile-time")) {
                            Basis.usingMavenCentral()
                                    .addDependencies(project.dependencies(AvailableDuring.BENCH_COMPILE_TIME))
                                    .build()
                                    .printTree();
                        }
                    }
                    Basis.usingMavenCentral()
                            .addDependencies(project.dependencies(AvailableDuring.NORMAL_COMPILE_TIME))
                            .build()
                            .printTree();
                }

                case "path" -> {
                    System.out.println(path(Basis.usingMavenCentral()
                            .addDependencies(project.dependencies(AvailableDuring.BENCH_COMPILE_TIME))
                            .build()));
                }
            }
        }

        var basis = Basis.builder()
                .addDependency(new MavenDependency("org.apache", "whatever", "12.14"))
                .addPath(SRC_CLASSES_DIR)
                .build();
    }
}
