package dev.mccue.jproject.model;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

import static dev.mccue.jproject.model.Basis.Requires.*;

/**
 * A "basis" is the set of dependencies and flags to use when constructing
 * a JVM runtime.
 */
public final class Basis implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final Object deps;
    private final Object repos;
    private final List<Path> paths;

    private record BasisProxy(
            Object deps,
            Object repos,
            List<String> paths
    ) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        @Serial
        private Object readResolve() throws ObjectStreamException {
            return new Basis(this);
        }
    }

    @Serial
    private Object writeReplace() throws ObjectStreamException {
        return new BasisProxy(
                this.deps,
                this.repos,
                this.paths.stream().map(Path::toString).toList()
        );
    }

    private Basis(BasisProxy serializationProxy) {
        this.deps = serializationProxy.deps();
        this.repos = serializationProxy.repos();
        this.paths = serializationProxy.paths().stream()
                .map(Path::of)
                .toList();
    }

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

        this.deps = deps;
        this.repos = repos;
        this.paths = List.copyOf(builder.paths);
    }

    /**
     * @return The path containing all the dependencies that should be placed on
     * the classpath and/or modulepath at startup.
     */
    public String path() {
        return (String) JOIN_CLASSPATH.invoke(
                VEC.invoke(this.pathRoots().stream().map(Path::toString).toList())
        );
    }

    public List<Path> pathRoots() {
        var classpathMap = (IFn) MAKE_CLASSPATH_MAP.invoke(

                HASH_MAP.invoke(
                        KEYWORD.invoke("paths"),
                        VEC.invoke(this.paths.stream().map(Path::toString).toList())
                ),
                RESOLVE_DEPS.invoke(
                        HASH_MAP.invoke(
                                KEYWORD.invoke("deps"), this.deps,
                                KEYWORD.invoke("mvn/repos"), this.repos
                        ),
                        HASH_MAP.invoke()
                ),
                null
        );
        var roots = (List<?>) classpathMap.invoke(KEYWORD.invoke("classpath-roots"));
        return roots.stream().map(root -> Path.of((String) root)).toList();
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
        static final IFn MAKE_CLASSPATH_MAP;
        static final IFn JOIN_CLASSPATH;
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
            MAKE_CLASSPATH_MAP = Clojure.var("clojure.tools.deps.alpha", "make-classpath-map");
            JOIN_CLASSPATH = Clojure.var("clojure.tools.deps.alpha", "join-classpath");
            PRINT_TREE = Clojure.var("clojure.tools.deps.alpha", "print-tree");
        }
    }

    public static final class Builder {
        private final List<MavenDependency> dependencies;
        private final List<MavenRepository> repositories;
        private final List<Path> paths;

        private Builder() {
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


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Basis basis = (Basis) o;
        return Objects.equals(deps, basis.deps)
                && Objects.equals(repos, basis.repos)
                && Objects.equals(paths, basis.paths);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deps, repos, paths);
    }
}
