package dev.mccue.project;

import picocli.CommandLine;

import java.io.PrintWriter;
import java.util.spi.ToolProvider;

public final class JProjectToolProvider implements ToolProvider {
    @Override
    public String name() {
        return "jproject";
    }

    @Override
    public int run(PrintWriter out, PrintWriter err, String... args) {
        return new CommandLine(new CliMain(out, err)).execute(args);
    }
}
