package dev.mccue.jproject;

import java.nio.file.Path;

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
     * The root directory to use for the output of build steps.
     */
    public static final Path TARGET_DIR = Path.of("target");

    /**
     *
     */
    public static final Path CLASSES_DIR = Path.of(TARGET_DIR.toString(), "classes");

    /**
     *
     */
    public static final Path TEST_CLASSES_DIR = Path.of(TARGET_DIR.toString(), "test", "classes");


    /**
     *
     */
    public static final Path JAR_DIR = Path.of(TARGET_DIR.toString(), "jar");


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
