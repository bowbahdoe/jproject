package dev.mccue.jproject.model;

import java.util.List;

/**
 * A dependency that comes from Maven. By far the most common case.
 */
public record MavenDependency(Coordinate coordinate, String version, List<Coordinate> exclusions) {
    public record Coordinate(String groupId, String artifactId) {}
}
