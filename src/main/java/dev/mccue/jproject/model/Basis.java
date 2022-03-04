package dev.mccue.jproject.model;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import static dev.mccue.jproject.model.Basis.Requires.*;

/**
 * A "basis" is the set of dependencies and flags to use when constructing
 * a JVM runtime.
 */
public final class Basis {
    private final List<String> jvmArgs;
    private final Object deps;
    private final Object repos;
    private final List<Path> paths;

    private Basis(Builder builder) {

        var deps = HASH_MAP.invoke();
        for (var dependency : builder.dependencies) {
            var coord = SYMBOL.invoke(
                    dependency.coordinate().groupId(),
                    dependency.coordinate().artifactId()
            );
            var map = HASH_MAP.invoke(
                    KEYWORD.invoke("mvn/version"),
                    dependency.version()
            );
            if (!dependency.exclusions().isEmpty()) {
                var exclusions = VECTOR.invoke();
                for (var exclusion : dependency.exclusions()) {
                    exclusions = CONJ.invoke(exclusions, SYMBOL.invoke(
                            exclusion.groupId(),
                            exclusion.artifactId()
                    ));
                }
                map = ASSOC.invoke(map, KEYWORD.invoke("exclusions"), exclusions);
            }
            deps = ASSOC.invoke(deps, coord, map);
        }

        var repos = HASH_MAP.invoke();
        for (var repository : builder.repositories) {
            repos = ASSOC.invoke(
                    repos,
                    repository.name(),
                    HASH_MAP.invoke(
                            KEYWORD.invoke("url"),
                            repository.url().toString()
                    )
            );
        }


        this.jvmArgs = List.copyOf(builder.jvmArgs);
        this.deps = deps;
        this.repos = repos;
        this.paths = List.copyOf(builder.paths);
    }

    /**
     * @return The path containing all the dependencies that should be placed on
     * the classpath and/or modulepath at startup.
     */
    public String path() {
        return (String) MAKE_CLASSPATH.invoke(
                RESOLVE_DEPS.invoke(
                        HASH_MAP.invoke(
                                KEYWORD.invoke("deps"), this.deps,
                                 KEYWORD.invoke("mvn/repos"), this.repos
                        ),
                        HASH_MAP.invoke()
                ),
                VEC.invoke(this.paths.stream().map(Path::toString).toList()),
                null
        );
    }

    /**
     * Prints the tree of dependencies.
     */
    public void printTree() {
        PRINT_TREE.invoke(
                RESOLVE_DEPS.invoke(
                        HASH_MAP.invoke(
                                KEYWORD.invoke("deps"), this.deps,
                                KEYWORD.invoke("mvn/repos"), this.repos
                        ),
                        HASH_MAP.invoke()
                )
        );
    }

    /**
     * @return The args to pass to the JVM.
     */
    public List<String> jvmArgs() {
        return List.copyOf(this.jvmArgs);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder usingMavenCentral() {
        return new Builder().addRepository(MavenRepository.MAVEN_CENTRAL);
    }

    static final class Requires {
        static final IFn KEYWORD;
        static final IFn SYMBOL;

        static final IFn VEC;
        static final IFn VECTOR;
        static final IFn CONJ;

        static final IFn HASH_MAP;
        static final IFn ASSOC;

        static final IFn RESOLVE_DEPS;
        static final IFn MAKE_CLASSPATH;
        static final IFn PRINT_TREE;

        static {
            KEYWORD = Clojure.var("clojure.core", "keyword");
            SYMBOL = Clojure.var("clojure.core", "symbol");


            VEC = Clojure.var("clojure.core", "vec");
            VECTOR = Clojure.var("clojure.core", "vector");
            CONJ = Clojure.var("clojure.core", "conj");

            HASH_MAP = Clojure.var("clojure.core", "hash-map");
            ASSOC = Clojure.var("clojure.core", "assoc");

            var REQUIRE = Clojure.var("clojure.core", "require");
            REQUIRE.invoke(Clojure.read("[clojure.tools.deps.alpha]"));
            RESOLVE_DEPS = Clojure.var("clojure.tools.deps.alpha", "resolve-deps");
            MAKE_CLASSPATH = Clojure.var("clojure.tools.deps.alpha", "make-classpath");
            PRINT_TREE = Clojure.var("clojure.tools.deps.alpha", "print-tree");
        }
    }

    public static final class Builder {
        private final List<String> jvmArgs;
        private final List<MavenDependency> dependencies;
        private final List<MavenRepository> repositories;
        private final List<Path> paths;

        private Builder() {
            this.jvmArgs = new ArrayList<>();
            this.dependencies = new ArrayList<>();
            this.repositories = new ArrayList<>();
            this.paths = new ArrayList<>();
        }

        public Builder addDependency(MavenDependency dependency) {
            this.dependencies.add(dependency);
            return this;
        }

        public Builder addDependencies(List<MavenDependency> dependencies) {
            this.dependencies.addAll(dependencies);
            return this;
        }

        public Builder addRepository(MavenRepository repository) {
            this.repositories.add(repository);
            return this;
        }

        public Builder addPath(Path path) {
            this.paths.add(path);
            return this;
        }

        public Basis build() {
            return new Basis(this);
        }
    }
}
