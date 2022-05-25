package dev.mccue.jproject.model;

import java.io.Serial;
import java.io.Serializable;
import java.net.URI;
import java.util.Objects;

/**
 * A maven repository is a place to check for artifacts.
 */
public record MavenRepository(String name, URI uri) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public MavenRepository {
        Objects.requireNonNull(name, "name should not be null");
        Objects.requireNonNull(uri, "uri should not be null");
    }

    /**
     * The primary repository of Java artifacts.
     */
    public static final MavenRepository MAVEN_CENTRAL = new MavenRepository(
            "central", URI.create("https://repo1.maven.org/maven2/")
    );

    /**
     * Jitpack is a repository that will automatically build and package
     * artifacts from repos hosted on git.
     */
    public static final MavenRepository JITPACK = new MavenRepository(
            "jitpack", URI.create("https://jitpack.io/")
    );

    /**
     * The Clojars repository is the place to use when including artifacts
     * from the Clojure ecosystem.
     */
    public static final MavenRepository CLOJARS = new MavenRepository(
            "clojars", URI.create("https://repo.clojars.org/")
    );

}