

package dev.mccue.jproject.model;

public sealed interface ModuleKind {
    record Library() implements ModuleKind {}
    record Application(String mainClass) implements ModuleKind {}
}