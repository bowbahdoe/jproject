package dev.mccue.jproject.model;

import com.moandjiezana.toml.Toml;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public record ApplicationModule(String mainClass, Map<MavenDependency, EnumSet<DependencyScope>> deps) {
    public static ApplicationModule fromFile(Path file) throws ConstructionException {
        var toml = new Toml();
        toml.read(file.toFile());

        if (!toml.containsTable("module")) {
            throw new ConstructionException("Missing [module]");
        }
        var application = toml.getTable("module");

        final String mainClass;
        try {
            mainClass = application.getString("main-class");
            if (mainClass == null) {
                throw new ConstructionException("Missing \"main-class\" in [application]");
            }
            else if ("".equals(mainClass.trim())) {
                throw new ConstructionException("\"main-class\" in [application] must be non-empty");
            }
        } catch (ClassCastException e) {
            throw new ConstructionException("\"main-class\" in [application] must be a String.");
        }

        if (!toml.containsTable("dependencies")) {
            throw new ConstructionException("Missing [dependencies]");
        }
        var dependencies = toml.getTable("dependencies");

        if (!toml.containsTable("test-only-dependencies")) {
            throw new ConstructionException("Missing [test-only-dependencies]");
        }
        var testOnlyDependencies = toml.getTable("test-only-dependencies");

        if (!toml.containsTable("compile-only-dependencies")) {
            throw new ConstructionException("Missing [compile-only-dependencies]");
        }
        var compileOnlyDependencies = toml.getTable("compile-only-dependencies");


        if (!toml.containsTable("runtime-only-dependencies")) {
            throw new ConstructionException("Missing [runtime-only-dependencies]");
        }
        var runtimeOnlyDependencies = toml.getTable("runtime-only-dependencies");

        var dependencyMap = new HashMap<MavenDependency, EnumSet<DependencyScope>>();

        Function<DependencyScope, Consumer<Map.Entry<String, Object>>> insert = scope -> entry -> {
            var key = entry.getKey();
            var version = entry.getValue();

            if (key.startsWith("\"") && key.endsWith("\"") ) {
                key = key.substring(1, key.length() - 1);
            }

            var split = key.split("/");
            var group = split[0];
            var artifact = split[1];

            var dependency = new MavenDependency(new MavenDependency.Coordinate(group, artifact), (String) version, List.of());
            dependencyMap.putIfAbsent(dependency, EnumSet.noneOf(DependencyScope.class));
            dependencyMap.get(dependency).add(scope);
        };

        dependencies.entrySet().forEach(insert.apply(DependencyScope.ALL));
        testOnlyDependencies.entrySet().forEach(insert.apply(DependencyScope.TEST));
        compileOnlyDependencies.entrySet().forEach(insert.apply(DependencyScope.COMPILE));
        runtimeOnlyDependencies.entrySet().forEach(insert.apply(DependencyScope.RUNTIME));

        return new ApplicationModule(mainClass, dependencyMap);
    }

    public List<MavenDependency> runtimeDependencies() {
        var deps = new ArrayList<MavenDependency>();
        this.deps().forEach((dep, scopes) -> {
            if (scopes.contains(DependencyScope.RUNTIME) || scopes.contains(DependencyScope.ALL)) {
                deps.add(dep);
            }
        });
        return deps;
    }

    public List<MavenDependency> testCompileDependencies() {
        var deps = new ArrayList<MavenDependency>();
        this.deps().forEach((dep, scopes) -> {
            if (scopes.contains(DependencyScope.COMPILE) ||
                    scopes.contains(DependencyScope.TEST) ||
                    scopes.contains(DependencyScope.ALL)
            ) {
                deps.add(dep);
            }
        });
        return deps;
    }

    public List<MavenDependency> testRuntimeDependencies() {
        var deps = new ArrayList<MavenDependency>();
        this.deps().forEach((dep, scopes) -> {
            if (scopes.contains(DependencyScope.RUNTIME) ||
                    scopes.contains(DependencyScope.TEST) ||
                    scopes.contains(DependencyScope.ALL)
            ) {
                deps.add(dep);
            }
        });
        return deps;
    }

    public List<MavenDependency> compileDependencies() {
        var deps = new ArrayList<MavenDependency>();
        this.deps().forEach((dep, scopes) -> {
            if (scopes.contains(DependencyScope.COMPILE) ||
                    scopes.contains(DependencyScope.ALL)
            ) {
                deps.add(dep);
            }
        });
        return deps;
    }



    public static final class ConstructionException extends Exception {
        private ConstructionException(String msg) {
            super(msg);
        }
    }
}
