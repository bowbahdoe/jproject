package dev.mccue.jproject.model;

import java.io.Serial;
import java.io.Serializable;
import java.net.URI;

/**
 * A maven repository is a place to check for artifacts.
 */
public record MavenRepository(String name, URI url) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final MavenRepository MAVEN_CENTRAL = new MavenRepository(
            "central", URI.create("https://repo1.maven.org/maven2/")
    );
    public static final MavenRepository CLOJARS = new MavenRepository(
            "clojars", URI.create("https://repo.clojars.org/")
    );
    public static final MavenRepository JITPACK = new MavenRepository(
            "jitpack", URI.create("https://jitpack.io/")
    );
}