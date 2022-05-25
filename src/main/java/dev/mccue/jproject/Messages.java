package dev.mccue.jproject;

public final class Messages {
    private Messages() {}

    public static final String USAGE = """
                    Java's package manager
                    
                    Usage: jproject <command> <options>
                    
                    Sample usages:
                    --------------
                        Create a new project named "example"
                            jproject new example
                        Run a project
                            jproject run
                        Run tests for a project
                            jproject test
                    
                    Available commands:
                        new      Create a new Java project
                        run      Run the project
                        test     Run JUnit tests
                        bench    Run JMH Benchmarks
                        doc      Generate Javadocs
                        idea     Generate scaffolding to open in IntelliJ
                        path     Echo the path that will be used to resolve dependencies
                        tree     Show the dependency tree of the current project
                        
                    See 'jproject help <command>' for more information on a specific command.
                    """;
}
