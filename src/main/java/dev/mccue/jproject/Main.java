package dev.mccue.jproject;

import dev.mccue.jproject.ant.AntPrettyMaker;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

public final class Main {

    private Main() {}

    static void printHelp() {
        System.out.println(Messages.USAGE);
    }

    public static void main(String[] args) throws Exception {

        // TODO: Write packaged ivy and ant into these directories
        var antDir = Path.of("~/.jproject/ant/");
        var ivyDir = Path.of("~/.jproject/ivy/");


        Files.deleteIfExists(Path.of("build.xml"));
        Files.deleteIfExists(Path.of("ivy.xml"));

        var buildXml = Path.of("build.xml");
        Files.writeString(buildXml, new BuildXml().contents());

        var ivyXml = Path.of("ivy.xml");
        Files.writeString(ivyXml, new IvyXml().contents());

        var antCmd = new String(new ProcessBuilder("which", "ant")
                .start()
                .getInputStream()
                .readAllBytes()
        ).trim();

        String[] command = { antCmd, "re" };

        var ant = new ProcessBuilder(command)
                .start();
        new AntPrettyMaker(ant).run();
        System.out.println(ant.waitFor());

        var junitRunner = "junit-platform-console-standalone-1.8.2.jar";
        var dump = Path.of(System.getProperty("user.home"), ".jproject", "external", junitRunner);
        Files.createDirectories(dump.getParent());
        Files.write(
                dump,
                Objects.requireNonNull(
                    Main.class.getResourceAsStream("/" + junitRunner),
                    "JUnit runner should be on the classpath"
                ).readAllBytes()
        );

        new ProcessBuilder("java", "-jar", dump.toString(), "--help")
                .inheritIO()
                .start()
                .waitFor();

        Files.delete(Path.of("build.xml"));
        Files.delete(Path.of("ivy.xml"));

        if (args.length == 0) {
            // printHelp();
            // System.exit(0);
        }
        else {
            var subcommand = args[0];
            switch (subcommand) {
                // Create a new project
                case "new":
                    break;
                // Clean all cached resources
                case "clean":
                    break;
                // Compile all modules
                case "compile":
                    break;
                // Run the project
                case "r":
                case "run":
                    System.out.println("run");
                    break;
                // Run tests with junit
                case "t":
                case "test":
                    System.out.println("test");
                    break;
                // Generate docs with javadoc
                case "doc":
                    break;
                // Run benchmarks with JMH
                case "bench":
                    break;
                // Publish a library to maven central
                case "publish":
                    break;
                // Print out the tree of dependencies
                case "tree":

            }
        }
    }
}
