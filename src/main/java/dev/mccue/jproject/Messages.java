package dev.mccue.jproject;

public final class Messages {
    private Messages() {}

    // older sketch of the idea
    public static final String USAGE = """
                    Java's package manager
                    
                    Usage: jproject <options> <tool> <tool-options>
                    
                    Sample usages:
                    --------------
                        Run a single source-file program
                            jproject java -cp ,,, src/com/company/MainClass.java
                        Execute a class
                            jproject java -cp ,,, com.company.MainClass
                        Execute a class with an alias
                            jproject --alias=build java -cp ,,, com.company.MainClass
                        Run javac with the project module-path
                            jproject javac --module-path ,,, -d target/classes src/*
                    
                    <options>:
                    -V, --version                Print version info and exit
                    -h, --help                   Print help information and exit
                    --list                       List all available tools
                    
                    Available tools (see all tools with --list):
                        new        Create a new Java module
                        run        Run the project
                        test       Run JUnit tests
                        bench      Run JMH Benchmarks
                        doc        Generate Javadocs
                        publish    Publish module to a repository
                        
                        eject      Exit from the template to the underlying tools
                        
                    See 'jproject help <command>' for more information on a specific command.
                    """;

}
