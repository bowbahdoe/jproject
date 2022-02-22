package dev.mccue.jproject.model;

public record SemVer(
        int major,
        int minor,
        int patch
) {
    public SemVer {
        if (major < 0) {
            throw new IllegalArgumentException("Major must be positive");
        }
        if (minor < 0) {
            throw new IllegalArgumentException("Minor must be positive");
        }
        if (patch < 0) {
            throw new IllegalArgumentException("Patch must be positive");
        }
    }
}