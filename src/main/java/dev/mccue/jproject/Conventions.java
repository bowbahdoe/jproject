package dev.mccue.jproject;

import dev.mccue.jproject.model.MavenDependency;

import java.nio.file.Path;
import java.util.List;

public final class Conventions {
    private Conventions() {}

    /**
     * The directory where source code and resources for
     * the main application are stored.
     */
    public static final Path SRC_DIR = Path.of("src");

    /**
     * The directory where source code and resources for
     * the application's tests are stored.
     */
    public static final Path TEST_DIR = Path.of("test");

    /**
     * The directory where source code and resources for
     * the application's benchmarks are stored
     */
    public static final Path BENCH_DIR = Path.of("bench");

    /**
     * The root directory to use for the output of build steps.
     */
    public static final Path TARGET_DIR = Path.of("target");

    /**
     * The directory to put classes that came from "src"
     */
    public static final Path SRC_CLASSES_DIR = Path.of(TARGET_DIR.toString(), "src", "classes");

    /**
     * The directory to put classes that came from "src"
     */
    public static final Path SRC_GENERATED_SOURCES_DIR = Path.of(TARGET_DIR.toString(), "src", "generated-sources");

    /**
     * The directory to put classes that came from "test"
     */
    public static final Path TEST_CLASSES_DIR = Path.of(TARGET_DIR.toString(), "test", "classes");

    /**
     * The directory to put classes that came from "src"
     */
    public static final Path TEST_GENERATED_SOURCES_DIR = Path.of(TARGET_DIR.toString(), "test", "generated-sources");

    /**
     * The directory to put classes that came from "bench"
     */
    public static final Path BENCH_CLASSES_DIR = Path.of(TARGET_DIR.toString(), "test", "bench");

    /**
     * The directory to put classes that came from "src"
     */
    public static final Path BENCH_GENERATED_SOURCES_DIR = Path.of(TARGET_DIR.toString(), "test", "bench", "generated-sources");

    /**
     *
     */
    public static final Path JAR_DIR = Path.of(TARGET_DIR.toString(), "jar");

    public static final Path NORMAL_JAR_FILE = Path.of(JAR_DIR.toString(), "app.jar");


    public static final Path TEST_JAR_FILE = Path.of(JAR_DIR.toString(), "test.jar");


    public static final Path BENCH_JAR_FILE = Path.of(JAR_DIR.toString(), "bench.jar");
    /**
     * The directory where any tool specific files will be stored.
     */
    public static final Path TOOL_DIRECTORY = Path.of(
            System.getProperty("user.home"),
            "/.jproject/"
    );

    /**
     * The path where the pre-packaged JUnit test runner jar will be expanded.
     */
    public static final Path JUNIT_RUNNER_PATH = Path.of(
            TOOL_DIRECTORY.toString(),
            "junit-platform-console-standalone-1.8.2.jar"
    );

    public static final List<MavenDependency> JUNIT_RUNNER_DEPENDENCIES = List.of(
            new MavenDependency(
                    "org.junit.platform",
                    "junit-platform-console",
                    "1.8.2"
            )
    );

    public static final List<MavenDependency> GOOGLE_JAVA_FORMAT_DEPENDENCIES = List.of(
            new MavenDependency(
                    "com.google.googlejavaformat",
                    "google-java-format",
                    "1.13.0"
            )
    );

    /**
     * The path where the pre-packaged JUnit test runner jar will be expanded.
     */
    public static final Path GOOGLE_JAVA_FORMAT_PATH = Path.of(
            TOOL_DIRECTORY.toString(),
            "google-java-format-1.14.0-all-deps.jar"
    );

    /**
     * The path where the pre-packaged Jacoco ant plugin will be placed.
     */
    public static final Path JACOCO_AGENT_PATH = Path.of(
            TOOL_DIRECTORY.toString(),
            "org.jacoco.agent-0.8.7.jar"
    );

    /**
     * Path to search for jproject.toml
     */
    public static final Path JPROJECT_TOML_PATH =
            Path.of("./jproject.toml");

    /**
     * Directory to download dependencies into
     */
    public static final Path DEPENDENCIES_PATH = Path.of("lib");
}
