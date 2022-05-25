package dev.mccue.jproject.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * A dependency that comes from a Maven Repository. By far the most common case.
 */
public record MavenDependency(Coordinate coordinate, String version, List<Coordinate> exclusions)
        implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public MavenDependency(Coordinate coordinate, String version, List<Coordinate> exclusions) {
        this.coordinate = Objects.requireNonNull(coordinate, "coordinate should not be null");
        this.version = Objects.requireNonNull(version, "version should not be null");
        this.exclusions = List.copyOf(
                Objects.requireNonNull(exclusions, "exclusions should not be null")
        );
    }

    public MavenDependency(String groupId, String artifactId, String version) {
        this(new Coordinate(groupId, artifactId), version, List.of());
    }

    public record Coordinate(String groupId, String artifactId) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        public Coordinate {
            Objects.requireNonNull(groupId, "groupId should not be null");
            Objects.requireNonNull(artifactId, "artifactId should not be null");
        }
    }
}
