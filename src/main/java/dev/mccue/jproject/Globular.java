package dev.mccue.jproject;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

public class Globular {
    static List<Path> glob(String pattern) throws IOException {
        var matcher = FileSystems
                .getDefault()
                .getPathMatcher("glob:" + pattern);
        try (var files = Files.walk(Path.of("."))) {
            return files.filter(matcher::matches).toList();
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println(glob("./src/**/*.java"));
    }
}
