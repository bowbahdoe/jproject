package dev.mccue.jproject.ant;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * This class is a bad idea generally speaking, but also a fun one.
 *
 * <p>
 * Wraps the output of a call to Ant and formats each line that goes out
 * in a "nicer" way. This lets us hide that we are using Ant, Ivy, etc.
 * under the hood.
 *
 * <p>
 * Why do this? Well, I don't like the output of Ant. Kinda simple as that.
 * It is hypothetically brittle, but I am planning on locking the versions
 * of ant and ivy by having them be included
 */
public final class AntPrettyMaker {
    /**
     * Handle to the running ant process output.
     */
    private final BufferedInputStream antProcessOutput;

    /**
     * Handle to the running ant process error.
     */
    private final BufferedInputStream antProcessError;

    /**
     * All the different processors for lines to try.
     */
    private final List<ProcessedOutputFactory> processors = List.of(
            // BuildFile::fromLine
    );

    public AntPrettyMaker(Process antProcess) {
        this.antProcessOutput = new BufferedInputStream(antProcess.getInputStream());
        this.antProcessError = new BufferedInputStream(antProcess.getErrorStream());
    }

    public void run() {
        BiFunction<InputStream, PrintStream, Runnable> processFactory = (in, out) -> () -> {
            var scanner = new Scanner(in);

            lines:
            while (scanner.hasNextLine()) {
                var line = scanner.nextLine();
                for (var processor : this.processors) {
                    var processed = processor.fromLine(line).orElse(null);
                    if (processed != null) {
                        processed.translate(out);
                        continue lines;
                    }
                }
                out.println(line);
            }
        };

        var out = new Thread(processFactory.apply(this.antProcessOutput, System.out));
        var err = new Thread(processFactory.apply(this.antProcessError, System.err));

        out.start();
        err.start();

        try {
            out.join();
            err.join();
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    interface ProcessedOutput {
        void translate(PrintStream printTo);
    }

    interface ProcessedOutputFactory {
        Optional<? extends ProcessedOutput> fromLine(String line);
    }

    record BuildFile(String file) implements ProcessedOutput{
        static Optional<? extends ProcessedOutput> fromLine(String line) {
            var prefix = "Buildfile: ";
            if (line.startsWith(prefix)) {
                return Optional.of(new BuildFile(line.substring(prefix.length())));
            }
            else {
                return Optional.empty();
            }
        }

        @Override
        public void translate(PrintStream printTo) {
            printTo.print("\033[1;32m");
            printTo.print("[USING BUILD FILE]: " + file);
            System.out.println("\033[0m");
        }
    }
}
