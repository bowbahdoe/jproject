package dev.mccue.jproject;

import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;

import static dev.mccue.jproject.Conventions.*;

public final class Setup {
    private Setup() {}

    private static void crashOn(int code) {
        if (code != 0) {
            System.exit(code);
        }
    }

    /**
     * Sets up all the scaffolding for running jproject
     */
    public static void setUp() throws IOException {
        // Clear existing ~/.jproject
        if (!Files.exists(TOOL_DIRECTORY)) {
            TOOL_DIRECTORY.toFile().mkdirs();

            // Copy Google Java Format
            var googleJavaFormat = "google-java-format-1.14.0-all-deps.jar";
            Files.write(
                    GOOGLE_JAVA_FORMAT_PATH,
                    Objects.requireNonNull(
                            Main.class.getResourceAsStream("/" + googleJavaFormat),
                            "Google Java Format should be on the classpath"
                    ).readAllBytes()
            );

            // Copy Junit runner
            var junit = "junit-platform-console-standalone-1.8.2.jar";
            Files.write(
                    JUNIT_RUNNER_PATH,
                    Objects.requireNonNull(
                            Main.class.getResourceAsStream("/" + junit),
                            "JUnit Platform Console should be on the classpath"
                    ).readAllBytes()
            );

            // Copy jacoco ant plugin
            var jacoco = "org.jacoco.agent-0.8.7.jar";
            Files.write(
                    JACOCO_AGENT_PATH,
                    Objects.requireNonNull(
                            Main.class.getResourceAsStream("/" + jacoco),
                            "Jacoco Ant Plugin should be on the classpath"
                    ).readAllBytes()
            );
        }

    }
}
