package dev.mccue.jproject;

import dev.mccue.jproject.model.ApplicationModule;

import java.nio.file.Files;
import java.nio.file.Path;

public final class Main {

    private Main() {}

    private static void crashOn(int status) {
        if (status != 0) {
            System.exit(status);
        }
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

        // Clean up evidence of previous runs
        Files.deleteIfExists(Path.of("build.xml"));
        Files.deleteIfExists(Path.of("ivy.xml"));

        var buildXml = Path.of("build.xml");
        Files.writeString(buildXml, BuildXml.contents(project));

        var ivyXml = Path.of("ivy.xml");
        Files.writeString(ivyXml, IvyXml.contents(project));

        var antExeStr = Path.of(
                Conventions.ANT_DIRECTORY.toString(),
                "apache-ant-1.10.11",
                "bin",
                "ant"
        ).toString();


        if (args.length == 0) {
            System.out.println(Messages.USAGE);
        }
        else {
            var subcommand = args[0];
            switch (subcommand) {
                // Create a new project
                case "new":
                    break;
                // Clean all cached resources
                case "clean":
                    crashOn(new ProcessBuilder(antExeStr, "clean")
                            .inheritIO()
                            .start()
                            .waitFor());
                    break;
                // Compile all modules
                case "compile":
                    crashOn(new ProcessBuilder(antExeStr, "compile")
                            .inheritIO()
                            .start()
                            .waitFor());
                    break;
                // Run the project
                case "r":
                case "run":
                    crashOn(new ProcessBuilder(antExeStr, "run")
                            .inheritIO()
                            .start()
                            .waitFor());
                    System.out.println("run");
                    break;
                // Run tests with junit
                case "t":
                case "test":
                    System.out.println("test");
                    break;
                // Generate docs with javadoc
                case "doc":
                    System.err.println("doc: NOT YET IMPLEMENTED");
                    System.exit(1);
                // Run benchmarks with JMH
                case "bench":
                    System.err.println("bench: NOT YET IMPLEMENTED");
                    System.exit(1);
                // Publish a library to maven central
                case "publish":
                    System.err.println("publish: NOT YET IMPLEMENTED");
                    System.exit(1);
                // Print out the tree of dependencies
                case "tree":
                    crashOn(new ProcessBuilder(antExeStr, "tree")
                            .inheritIO()
                            .start()
                            .waitFor());
            }
        }

        Files.delete(ivyXml);
        Files.delete(buildXml);
    }
}
