package dev.mccue.jproject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

public class CompileTask {
    public static void compile(Path pwd) throws IOException, InterruptedException {
        // find jproject.toml file
        // assert jproject.toml
        // assert project structure
        // generate module-path to use for compilation
        var tempDir = Files.createTempDirectory(
                "build-" + Instant.now().toEpochMilli() + "-"
        );

        var a = Files.createSymbolicLink(
                Path.of(tempDir.toString()),
                Path.of(pwd.toString(), "src")
        );
        var s = Files.createSymbolicLink(
                Path.of(tempDir.toString(), "module.name", "module-info.java"),
                Path.of(pwd.toString(), "module-info.java")
        );

        Files.readAllLines(Path.of("Words"));
        new ProcessBuilder("ls", s.toString())
                .inheritIO()
                .start().waitFor();
        new ProcessBuilder("ls", a.toString())
                .inheritIO()
                .start().waitFor();
        // run javac to compile modules
        // package modules into modular jars

    }

    public static void main(String[] args) throws IOException, InterruptedException {
        compile(Path.of("ex"));
    }
}
