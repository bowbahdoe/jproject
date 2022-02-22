package dev.mccue.jproject;

public final class Commands {
    private Commands() {}

    public static void newProject(String[] args) {
        enum Type {
            LIB, APP
        }

        var type = Type.APP;
        for (int i = 1; i < args.length; i++) {
            if ("--app".equals(args[i])) {
                type = Type.APP;
            }
            if ("--lib".equals(args[i])) {
                type = Type.LIB;
            }
        }

        switch (type) {
            case APP -> {

            }
        }
    }
}
