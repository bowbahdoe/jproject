package dev.mccue.jproject.task;

import javax.lang.model.SourceVersion;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Handle to a running JVM loaded with the seed program
 * that can be re-used for multiple command invocations.
 *
 * This might also just decide to shell out every time.
 * The purpose of this as an interface is to allow whatever
 * approach is wanted.
 *
 * What this (fortunately or unfortunately) means is that
 * the tool is basically always running "forked".
 */
public final class Jvm implements AutoCloseable {
    // https://stackoverflow.com/questions/36899278/how-do-i-find-the-location-of-the-specific-java-exe-that-my-jvm-was-created-by
    // Default to run with same java that jproject was started with.
    private static final String DEFAULT_JAVA_COMMAND = Path.of(
            System.getProperty("java.home"),
            "bin",
            "java"
    ).toString();

    private final String javaCommand;

    private Jvm(String javaCommand) {
        this.javaCommand = javaCommand;
    }

    public static Jvm create() {
        return new Jvm(DEFAULT_JAVA_COMMAND);
    }

    public static Jvm create(String javaCommand) {
        return new Jvm(javaCommand == null ? DEFAULT_JAVA_COMMAND : javaCommand);
    }

    private void crashOn(int code) {
        if (code != 0) {
            System.exit(code);
        }
    }

    private String runAndCaptureOutput(String code) {
        try {
            var tempSourceFile = Files.createTempFile("Main", ".java");
            Files.writeString(tempSourceFile, code);
            var tempOutFile = Files.createTempFile("out", "log");
            var process = new ProcessBuilder(this.javaCommand, tempSourceFile.toString())
                    .inheritIO()
                    .redirectOutput(tempOutFile.toFile())
                    .start();
            crashOn(process.waitFor());
            return Files.readString(tempOutFile);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void run(String code) {
        try {
            var tempSourceFile = Files.createTempFile("Main", ".java");
            Files.writeString(tempSourceFile, code);
            var process = new ProcessBuilder(this.javaCommand, tempSourceFile.toString())
                    .inheritIO()
                    .start();
            crashOn(process.waitFor());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return The source version available in the given JVM.
     */
    public SourceVersion sourceVersion() {
        var code = """
                import javax.lang.model.SourceVersion;
                
                public final class Main {
                    public static void main(String[] args) {
                        System.out.print(SourceVersion.latest());
                    }
                }
                """;
        return SourceVersion.valueOf(runAndCaptureOutput(code));
    }

    public List<String> discoverTools() {
        var code = """   
                import java.util.ServiceLoader;
                import java.util.spi.ToolProvider;
                
                public final class Main {
                    public static void main(String[] args) {
                        ServiceLoader
                                .load(ToolProvider.class)
                                .stream()
                                .map(ServiceLoader.Provider::get)
                                .map(ToolProvider::name)
                                .forEach(System.out::println);
                    }
                }
                """;
        return List.copyOf(Arrays.asList(runAndCaptureOutput(code).split("\n")));
    }

    public void runTool(String toolName, List<String> args) {
        var code = """   
                import java.util.spi.ToolProvider;
                
                public final class Main {
                    public static void main(String[] args) {
                        var tool = ToolProvider.findFirst(args[0]).orElseThrow();
                        String[] argsClone = new String[args.length - 1];
                        for (int i = 1; i < args.length; i++) {
                            argsClone[i - 1] = args[i];
                        }
                        
                        tool.run(System.out, System.err, argsClone);
                    }
                }
                """;

        try {
            var tempSourceFile = Files.createTempFile("Main", ".java");
            Files.writeString(tempSourceFile, code);
            String[] cmdArgs = new String[args.size() + 3];
            cmdArgs[0] = this.javaCommand;
            cmdArgs[1] = tempSourceFile.toString();
            cmdArgs[2] = toolName;
            for (int i = 0; i < args.size(); i++) {
                cmdArgs[i + 3] = args.get(i);
            }
            var process = new ProcessBuilder(cmdArgs)
                    .inheritIO()
                    .start();
            crashOn(process.waitFor());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {

    }

    public static void main(String[] args) {
        var jvm = Jvm.create();
        System.out.println(jvm.sourceVersion());
        System.out.println(jvm.discoverTools());
        jvm.runTool("javac", List.of("--version"));
    }
}
