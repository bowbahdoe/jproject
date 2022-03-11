package dev.mccue.jproject.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * A dependency that comes from Maven. By far the most common case.
 */
public record MavenDependency(Coordinate coordinate, String version, List<Coordinate> exclusions)
        implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public MavenDependency(Coordinate coordinate, String version, List<Coordinate> exclusions) {
        this.coordinate = coordinate;
        this.version = version;
        this.exclusions = List.copyOf(exclusions);
    }

    public MavenDependency(String groupId, String artifactId, String version) {
        this(new Coordinate(groupId, artifactId), version, List.of());
    }

    public record Coordinate(String groupId, String artifactId) implements Serializable {}
}
