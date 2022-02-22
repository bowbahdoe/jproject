package dev.mccue.jproject.model;

import java.net.URI;

/**
 * A maven repository is a place to check for artifacts.
 */
public record MavenRepository(URI url) {
    public static final MavenRepository MAVEN_CENTRAL = new MavenRepository(
            URI.create("https://repo1.maven.org/maven2/")
    );
    public static final MavenRepository CLOJARS = new MavenRepository(
            URI.create("https://repo.clojars.org/")
    );
    public static final MavenRepository JITPACK = new MavenRepository(
            URI.create("https://jitpack.io/")
    );
}