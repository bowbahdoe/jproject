package dev.mccue.jproject;

import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
    public static void setUp() throws IOException, InterruptedException {
        // Clear existing ~/.jproject
        FileUtils.deleteDirectory(TOOL_DIRECTORY.toFile());

        // Copy and extract Apache Ant
        var ant = "apache-ant-1.10.11.zip";
        try {
            Files.write(
                    Path.of(ant),
                    Objects.requireNonNull(
                            Main.class.getResourceAsStream("/" + ant),
                            "Ant should be on the classpath"
                    ).readAllBytes()
            );

            Files.createDirectories(ANT_DIRECTORY);
            crashOn(
                    new ProcessBuilder(
                            // TODO: "unzip" only works on mac
                            List.of("unzip", Path.of(ant).toString(), "-d", ANT_DIRECTORY.toString())
                    )
                            .inheritIO()
                            .start()
                            .waitFor()
            );
        }
        finally {
            Files.delete(Path.of(ant));
        }

        // Copy and extract Apache Ivy
        var ivy = "apache-ivy-2.5.0.zip";
        try {
            Files.write(
                    Path.of(ivy),
                    Objects.requireNonNull(
                            Main.class.getResourceAsStream("/" + ivy),
                            "Ivy should be on the classpath"
                    ).readAllBytes()
            );

            Files.createDirectories(IVY_DIRECTORY);
            crashOn(
                    new ProcessBuilder(
                            // TODO: "unzip" only works on mac
                            List.of("unzip", Path.of(ivy).toString(), "-d", IVY_DIRECTORY.toString())
                    )
                            .inheritIO()
                            .start()
                            .waitFor()
            );
            Files.copy(
                    Path.of(IVY_DIRECTORY.toString(), "apache-ivy-2.5.0", "ivy-2.5.0.jar"),
                    Path.of(ANT_DIRECTORY.toString(), "apache-ant-1.10.11", "lib", "ivy.jar")
            );
        }
        finally {
            Files.delete(Path.of(ivy));
        }

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
        var jacoco = "org.jacoco.ant-0.8.7.jar";
        Files.write(
                JACOCO_ANT_PATH,
                Objects.requireNonNull(
                        Main.class.getResourceAsStream("/" + jacoco),
                        "Jacoco Ant Plugin should be on the classpath"
                ).readAllBytes()
        );
        Files.copy(
                JACOCO_ANT_PATH,
                Path.of(ANT_DIRECTORY.toString(), "apache-ant-1.10.11", "lib", "jacoco.jar")
        );
    }
}
