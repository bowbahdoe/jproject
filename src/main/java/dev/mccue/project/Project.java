package dev.mccue.project;

import java.util.*;

record Project(
        String defaultUsage,
        List<Dependency> dependencies,
        List<DependencySet> dependencySets,
        List<MavenRepository> repositories,
        List<ArgumentFile> argumentFiles
) {
    sealed interface Coordinate {
        record Maven(String groupId, String artifactId, String version) implements Coordinate {
        }

        record Path(String path) implements Coordinate {
        }

        record URL(String url) implements Coordinate {
        }
    }

    record Dependency(
            Coordinate coordinate,
            List<String> dependencySets,
            List<String> usages,
            List<String> repositories
    ) {
        Dependency(
                Coordinate coordinate,
                List<String> dependencySets,
                String usage,
                String repositories
        ) {
            List<String> repositoriesList;
            if (repositories == null) {
                repositoriesList = List.of();
            } else {
                repositoriesList = Arrays.stream(repositories.strip().split(","))
                        .map(String::strip)
                        .toList();
            }

            List<String> usageList;
            if (usage == null) {
                usageList = List.of();
            } else {
                usageList = Arrays.stream(usage.strip().split(","))
                        .map(String::strip)
                        .toList();
            }

            this(coordinate, dependencySets, usageList, repositoriesList);
        }

        static Dependency fromXML(XML dependency) {
            var groupId = XMLDecoder
                    .singleChildNamed(
                            dependency,
                            "groupId",
                            XMLDecoder::singleTextChild
                    );
            var artifactId = XMLDecoder
                    .singleChildNamed(
                            dependency,
                            "artifactId",
                            XMLDecoder::singleTextChild
                    );

            var version = XMLDecoder
                    .singleChildNamed(
                            dependency,
                            "version",
                            XMLDecoder::singleTextChild
                    );

            var path = XMLDecoder.singleChildNamed(
                    dependency,
                    "path",
                    XMLDecoder::singleTextChild
            );

            var url = XMLDecoder.singleChildNamed(
                    dependency,
                    "url",
                    XMLDecoder::singleTextChild
            );

            var usage = XMLDecoder.singleChildNamed(
                    dependency,
                    "usage",
                    XMLDecoder::singleTextChild
            );

            var repository = XMLDecoder.singleChildNamed(
                    dependency,
                    "repository",
                    XMLDecoder::singleTextChild
            );

            var dependencySets = XMLDecoder.singleChildNamed(
                    dependency,
                    "dependencySets",
                    element -> XMLDecoder.nodeStripBlankTextChildren(element).children()
                            .stream()
                            .map(child -> XMLDecoder.nodeStripBlankTextChildren(child).name())
                            .toList()
            );

            Coordinate coordinate;
            if (path != null) {
                if (groupId != null || artifactId != null || version != null) {
                    throw new RuntimeException("Cannot have groupId, artifactId, or version with path");
                }
                if (url != null) {
                    throw new RuntimeException("path and url cannot both be provided");
                }
                coordinate = new Coordinate.Path(path);
            } else if (url != null) {
                if (groupId != null || artifactId != null || version != null) {
                    throw new RuntimeException("Cannot have groupId, artifactId, or version with url");
                }
                coordinate = new Coordinate.URL(url);
            } else {
                if (groupId == null) {
                    throw new RuntimeException("missing groupId");
                }
                if (artifactId == null) {
                    throw new RuntimeException("missing artifactId");
                }
                if (version == null) {
                    throw new RuntimeException("missing version");
                }
                coordinate = new Coordinate.Maven(groupId, artifactId, version);
            }

            return new Dependency(
                    coordinate,
                    dependencySets,
                    usage,
                    repository
            );
        }
    }

    record DependencySet(
            String name,
            String extends_
    ) {
        static DependencySet fromXML(XML xml) {
            var dependencySetName = XMLDecoder.nodeName(xml);
            var extends_ = XMLDecoder.singleChildNamed(
                    xml,
                    "extends",
                    XMLDecoder::singleTextChild
            );
            return new DependencySet(dependencySetName, extends_);
        }
    }

    record MavenRepository(
            String name,
            String url,
            Authentication authentication
    ) {

        static MavenRepository fromXML(XML xml) {
            var name = XMLDecoder.nodeName(xml);
            var url = XMLDecoder
                    .singleChildNamed(
                            xml,
                            "url",
                            XMLDecoder::singleTextChild
                    );

            var authentication = XMLDecoder
                    .singleChildNamed(
                            xml,
                            "authentication",
                            Authentication::fromXML
                    );

            return new MavenRepository(
                    name, url, authentication
            );
        }

        record Authentication(String username, String password) {
            static Authentication fromXML(XML xml) {
                var username = XMLDecoder
                        .singleChildNamed(
                                xml,
                                "username",
                                XMLDecoder::singleTextChild
                        );
                var password = XMLDecoder
                        .singleChildNamed(
                                xml,
                                "authentication",
                                XMLDecoder::singleTextChild
                        );
                if (username == null || password == null) {
                    throw new RuntimeException("Authentication must specify both username and password");
                }

                return new Authentication(username, password);
            }
        }
    }

    record ArgumentFile(String name, List<TopLevelArgument> args) {
        sealed interface NestedArgument {
        }

        sealed interface TopLevelArgument {
        }

        record Argument(List<NestedArgument> nestedArgs) implements TopLevelArgument {
        }

        record DependencySet(String name) implements TopLevelArgument {
        }

        record Path(String path) implements TopLevelArgument, NestedArgument {
        }

        record Text(String value) implements NestedArgument {
        }

        List<String> toArgs(SequencedMap<String, String> getPath) {
            var stringifiedArgs = new ArrayList<String>();
            for (var arg : args) {
                switch (arg) {
                    case Path p -> stringifiedArgs.add(java.nio.file.Path.of(p.path()).toString());
                    case DependencySet dependencySet -> {
                        var path = getPath.get(dependencySet.name());
                        if (path != null) {
                            stringifiedArgs.add(path);
                        } else {
                            if (!dependencySet.name.equals("default")) {
                                throw new RuntimeException("No dependencySet named " + dependencySet.name + ". Available: " + getPath.keySet());
                            }
                        }
                    }
                    case Argument argument -> {
                        var sb = new StringBuilder();
                        for (var nested : argument.nestedArgs()) {
                            switch (nested) {
                                case Text t -> sb.append(t.value);
                                case Path p -> sb.append(java.nio.file.Path.of(p.path()));
                            }
                        }
                        stringifiedArgs.add(sb.toString());
                    }
                }
            }

            return stringifiedArgs;
        }

        static ArgumentFile fromXML(XML xml) {
            var name = XMLDecoder.nodeName(xml);
            var args = new ArrayList<TopLevelArgument>();
            var node = XMLDecoder.node(xml);
            for (var child : node.children()) {
                if (child instanceof XML.Node childNode) {
                    switch (childNode.name()) {
                        case "argument" -> {
                            var nestedArgs = new ArrayList<NestedArgument>();
                            for (var nestedArg : childNode.children()) {
                                switch (nestedArg) {
                                    case XML.Text(String value) -> {
                                        nestedArgs.add(new Text(value));
                                    }
                                    case XML.Node n -> {
                                        switch (n.name()) {
                                            case "path" -> {
                                                nestedArgs.add(new Path(
                                                        Objects.requireNonNullElse(XMLDecoder.singleTextChild(n), "").strip()
                                                ));
                                            }
                                            default -> {
                                                throw new RuntimeException("Unexpected node in <argument>: " + n.name());
                                            }
                                        }
                                    }

                                }
                            }

                            args.add(new Argument(nestedArgs));
                        }
                        case "path" -> {
                            args.add(new Path(
                                    Objects.requireNonNullElse(XMLDecoder.singleTextChild(childNode), "").strip()
                            ));
                        }
                        case "dependencySet" -> {
                            args.add(new DependencySet(
                                    Objects.requireNonNullElse(XMLDecoder.singleTextChild(childNode), "").strip()
                            ));
                        }
                        default -> {
                            throw new RuntimeException("Unexpected tag: " + childNode);
                        }
                    }
                }
            }

            return new ArgumentFile(name, args);
        }
    }

    static Project fromXML(XML xml) {
        var rootName = XMLDecoder.nodeName(xml);
        if (!"project".equals(rootName)) {
            throw new RuntimeException("Expected a <project> element");
        }

        XMLDecoder.assertOnlyNodesNamed(
                XMLDecoder.node(xml),
                List.of(
                        "defaultUsage",
                        "dependencies",
                        "dependencySets",
                        "mavenRepositories",
                        "argumentFiles"
                )
        );

        var defaultUsage = XMLDecoder
                .singleChildNamed(
                        xml,
                        "defaultUsage",
                        XMLDecoder::singleTextChild
                );

        var dependenciesList = new ArrayList<Dependency>();
        var dependencies = XMLDecoder
                .singleChildNamed(xml, "dependencies");
        if (dependencies != null) {
            for (var dependency : XMLDecoder.allChildrenNamed(
                    dependencies,
                    "dependency"
            )) {
                dependenciesList.add(Dependency.fromXML(dependency));
            }
        }

        var dependencySetList = new ArrayList<DependencySet>();
        var dependencySets = XMLDecoder
                .singleChildNamed(xml, "dependencySets");
        if (dependencySets != null) {
            for (var dependencySet : XMLDecoder.node(dependencySets).children()) {
                if (dependencySet instanceof XML.Node) {
                    dependencySetList.add(DependencySet.fromXML(dependencySet));
                }
            }
        }

        var repositoryList = new ArrayList<MavenRepository>();
        var repositories = XMLDecoder
                .singleChildNamed(xml, "mavenRepositories");
        if (repositories != null) {
            for (var repository : XMLDecoder.node(repositories).children()) {
                if (repository instanceof XML.Node) {
                    repositoryList.add(MavenRepository.fromXML(repository));
                }
            }
        }

        var argumentFileList = new ArrayList<ArgumentFile>();
        var argumentFiles = XMLDecoder
                .singleChildNamed(xml, "argumentFiles");
        if (argumentFiles != null) {
            for (var argumentFile : XMLDecoder.node(argumentFiles).children()) {
                if (argumentFile instanceof XML.Node) {
                    argumentFileList.add(ArgumentFile.fromXML(argumentFile));
                }
            }
        }

        return new Project(
                defaultUsage,
                dependenciesList,
                dependencySetList,
                repositoryList,
                argumentFileList
        );
    }
}