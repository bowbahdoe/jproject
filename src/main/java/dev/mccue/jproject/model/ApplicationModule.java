package dev.mccue.jproject.model;

import com.moandjiezana.toml.Toml;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public record ApplicationModule(String mainClass, Map<AvailableDuring, List<MavenDependency>> deps) {
    public static ApplicationModule fromFile(Path file) throws ConstructionException {
        var toml = new Toml();
        toml.read(file.toFile());

        if (!toml.containsTable("application")) {
            throw new ConstructionException("Missing [application]");
        }
        var application = toml.getTable("application");

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

        if (!toml.containsTable("bench-only-dependencies")) {
            throw new ConstructionException("Missing [bench-only-dependencies]");
        }
        var benchOnlyDependencies = toml.getTable("bench-only-dependencies");


        var dependencyMap = new HashMap<AvailableDuring, List<MavenDependency>>();

        Function<Scope, Consumer<Map.Entry<String, Object>>> insert = scope -> entry -> {
            var key = entry.getKey();
            var version = entry.getValue();

            if (key.startsWith("\"") && key.endsWith("\"") ) {
                key = key.substring(1, key.length() - 1);
            }

            var split = key.split("/");
            var group = split[0];
            var artifact = split[1];

            if (version instanceof String versionStr) {
                var dependency = new MavenDependency(new MavenDependency.Coordinate(group, artifact), versionStr, List.of());

                switch (scope) {
                    case NORMAL -> {
                        dependencyMap.putIfAbsent(AvailableDuring.NORMAL_RUN_TIME, new ArrayList<>());
                        dependencyMap.get(AvailableDuring.NORMAL_RUN_TIME).add(dependency);

                        dependencyMap.putIfAbsent(AvailableDuring.NORMAL_COMPILE_TIME, new ArrayList<>());
                        dependencyMap.get(AvailableDuring.NORMAL_COMPILE_TIME).add(dependency);
                    }
                    case TEST ->  {
                        dependencyMap.putIfAbsent(AvailableDuring.TEST_RUN_TIME, new ArrayList<>());
                        dependencyMap.get(AvailableDuring.TEST_RUN_TIME).add(dependency);

                        dependencyMap.putIfAbsent(AvailableDuring.TEST_COMPILE_TIME, new ArrayList<>());
                        dependencyMap.get(AvailableDuring.TEST_COMPILE_TIME).add(dependency);
                    }
                    case BENCH -> {
                        dependencyMap.putIfAbsent(AvailableDuring.BENCH_RUN_TIME, new ArrayList<>());
                        dependencyMap.get(AvailableDuring.BENCH_RUN_TIME).add(dependency);

                        dependencyMap.putIfAbsent(AvailableDuring.BENCH_COMPILE_TIME, new ArrayList<>());
                        dependencyMap.get(AvailableDuring.BENCH_COMPILE_TIME).add(dependency);
                    }
                }
            }

            if (version instanceof Toml versionToml) {
                var versionStr = Objects.requireNonNull(versionToml.getString("version"));

                var availableDuring = EnumSet.noneOf(AvailableDuring.class);
                versionToml.getList("available-at").forEach(available -> {
                    if ("run-time".equals(available)) {
                        availableDuring.add(switch (scope) {
                            case NORMAL -> AvailableDuring.NORMAL_RUN_TIME;
                            case TEST ->  AvailableDuring.TEST_RUN_TIME;
                            case BENCH -> AvailableDuring.BENCH_RUN_TIME;
                        });
                    }
                    else if ("compile-time".equals(available)) {
                        availableDuring.add(switch (scope) {
                            case NORMAL -> AvailableDuring.NORMAL_COMPILE_TIME;
                            case TEST ->  AvailableDuring.TEST_COMPILE_TIME;
                            case BENCH -> AvailableDuring.BENCH_COMPILE_TIME;
                        });
                    }
                });
                availableDuring.forEach(available -> {
                    var dependency = new MavenDependency(new MavenDependency.Coordinate(group, artifact), versionStr, List.of());
                    dependencyMap.putIfAbsent(available, new ArrayList<>());
                    dependencyMap.get(available).add(dependency);
                });

            }

        };

        dependencies.entrySet().forEach(insert.apply(Scope.NORMAL));
        testOnlyDependencies.entrySet().forEach(insert.apply(Scope.TEST));
        benchOnlyDependencies.entrySet().forEach(insert.apply(Scope.BENCH));
        return new ApplicationModule(mainClass, dependencyMap);
    }

    public List<MavenDependency> dependencies(AvailableDuring availableDuring) {
        return this.deps.getOrDefault(availableDuring, List.of());
    }

    public static final class ConstructionException extends Exception {
        private ConstructionException(String msg) {
            super(msg);
        }
    }
}
