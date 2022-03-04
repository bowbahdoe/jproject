package dev.mccue.jproject.model;

import java.util.List;

/**
 * A dependency that comes from Maven. By far the most common case.
 */
public record MavenDependency(Coordinate coordinate, String version, List<Coordinate> exclusions) {
    public MavenDependency(Coordinate coordinate, String version) {
        this(coordinate, version, List.of());
    }

    public MavenDependency(String groupId, String artifactId, String version) {
        this(new Coordinate(groupId, artifactId), version, List.of());
    }

    public record Coordinate(String groupId, String artifactId) {}
}
