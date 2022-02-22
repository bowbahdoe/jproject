package dev.mccue.jproject.model;

/**
 * A dependency that comes from Maven. By far the most common case.
 */
public record MavenDependency(String groupId, String artifactId, String version) {}
