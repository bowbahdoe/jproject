package dev.mccue.project;

import dev.mccue.json.Json;
import dev.mccue.json.JsonDecoder;
import dev.mccue.resolve.*;
import dev.mccue.resolve.maven.MavenCoordinate;
import dev.mccue.resolve.maven.MavenRepository;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import picocli.CommandLine;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

@CommandLine.Command(
        name = "jproject",
        mixinStandardHelpOptions = true,
        version = "2025.11.21",
        description = "Manages dependencies for the JVM projects."
)
public final class CliMain {
    private final PrintWriter out;
    private final PrintWriter err;

    @CommandLine.Option(
            names = {"--print-tree"},
            description = "Skip fetching artifacts and print the result of dependency resolution"
    )
    public boolean printTree = false;

    public CliMain(PrintWriter out, PrintWriter err) {
        this.out = out;
        this.err = err;
    }


    public CliMain() {
        this(new PrintWriter(System.out), new PrintWriter(System.err));
    }

    public static void main(String... args) {
        int exitCode = new CommandLine(new CliMain())
                .execute(args);

        System.exit(exitCode);
    }

    @CommandLine.Command(
            name = "init",
            description = "Create a new Java project in the current directory"
    )
    public int init() throws Exception {
        Files.writeString(
                Path.of("jproject.xml"),
                """
                <project>
                    <dependencies>
                    </dependencies>
                
                    <argumentFiles>
                        <run>
                            <dependencySet>default</dependencySet>
                            <path>src/Main.java</path>
                        </run>
                    </argumentFiles>
                </project>
                """
        );

        Files.createDirectories(Path.of("src"));
        Files.writeString(Path.of("src", "Main.java"),
                """
                        void main() {
                            IO.println("Hello, world");
                        }
                        """);

        Files.writeString(
                Path.of("run"),
                Path.of("src", "Main.java").toString()
        );

        try {
            var gitignore = new ArrayList<>(Files.readAllLines(Path.of(".gitignore")));
            if (!gitignore.contains("dependencySets")) {
                gitignore.add("dependencySets");
            }
            if (!gitignore.contains("run")) {
                gitignore.add("run");
            }
            Files.writeString(Path.of(".gitignore"), String.join("\n", gitignore));
        } catch (NoSuchFileException e) {
            Files.writeString(Path.of(".gitignore"), """
                    dependencySets
                    run
                    """);
        }

        return 0;
    }



    @CommandLine.Command(
            name = "new",
            description = "Create a new Java project"
    )
    public int new_(
            File destination
    ) throws Exception {
        if (Files.exists(destination.toPath())) {
            err.println("destination `" + destination.toPath() + "` already exists");
            err.flush();
            return 1;
        }

        Files.writeString(
                Path.of(destination.toString(), "jproject.xml"),
                """
                <project>
                    <dependencies>
                    </dependencies>
                
                    <argumentFiles>
                        <run>
                            <dependencySet>default</dependencySet>
                            <path>src/Main.java</path>
                        </run>
                    </argumentFiles>
                </project>
                """
        );

        Files.createDirectories(Path.of(destination.toString(), "src"));
        Files.writeString(Path.of(destination.toString(), "src", "Main.java"),
                """
                        void main() {
                            IO.println("Hello, world");
                        }
                        """);

        Files.writeString(
                Path.of(destination.toString(), "run"),
                Path.of(destination.toString(), "src", "Main.java").toString()
        );


        return 0;
    }

    @CommandLine.Command(
            name = "download",
            description = "Downloads dependencies and creates argument files"
    )
    public int download(
            @CommandLine.Option(
                    names = {"--cache-path"},
                    description = "Path to use for caching the files fetched during dependency resolution"
            )
            File cachePath
    ) throws Exception {
        if (!Files.exists(Path.of("jproject.xml"))) {
            err.println("No jproject.xml found");
            err.flush();
            return 1;
        }

        var dbf = DocumentBuilderFactory.newDefaultInstance();
        var db = dbf.newDocumentBuilder();

        Document parsed;
        try {
            parsed = db.parse(new File("./jproject.xml"));
        } catch (IOException | SAXException e) {
            err.println(e.getMessage());
            err.flush();
            return 1;
        }

        var xml = XML.from(parsed);
        Project project;
        try {
            project = Project.fromXML(xml);
        } catch (XMLDecodeException e) {
            err.println(e.getMessage());
            err.flush();
            return 1;
        }


        var defaultUsage = Optional.ofNullable(project.defaultUsage())
                .map(Usage::new)
                .orElse(null);

        // Map from dependency set to the dependency set it extends
        var dependencySetExtends = new HashMap<String, String>();

        List<Project.DependencySet> projectDependencySets
                = Objects.requireNonNullElse(project.dependencySets(), List.of());
        for (var set : projectDependencySets) {
            if (set.extends_() != null) {
                dependencySetExtends.put(set.name(), set.extends_());
            }
        }

        record UsagesAndDep(List<Usage> usages, Dependency dependency) {
        }

        var dependencySetToPath = new LinkedHashMap<String, String>();
        var dependencySetToDeps = new HashMap<String, List<UsagesAndDep>>();
        var dependencies = project.dependencies();
        if (dependencies != null && !dependencies.isEmpty()) {
            var dependencySetsPath = Path.of("dependencySets");
            try {
                Files.walkFileTree(dependencySetsPath, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file,
                                                     BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir,
                                                              IOException e) throws IOException {
                        if (e == null) {
                            Files.delete(dir);
                            return FileVisitResult.CONTINUE;
                        } else {
                            throw e;
                        }
                    }

                });
            } catch (NoSuchFileException e) {
                // NoOp
            }

            var knownRepositories = new HashMap<String, MavenRepository>();
            knownRepositories.put("central", MavenRepository.central());
            knownRepositories.put("local", MavenRepository.local());

            if (project.repositories() != null) {
                project.repositories().forEach(repository -> {
                    knownRepositories.put(repository.name(), MavenRepository.remote(repository.url(), () -> {
                        var builder = HttpClient.newBuilder()
                                .followRedirects(HttpClient.Redirect.NORMAL);
                        var authentication = repository.authentication();
                        if (authentication != null) {
                            builder.authenticator(new Authenticator() {
                                @Override
                                protected PasswordAuthentication getPasswordAuthentication() {
                                    return new PasswordAuthentication(
                                            authentication.username(),
                                            authentication.password().toCharArray()
                                    );
                                }
                            });
                        }
                        return builder.build();
                    }));
                });
            }

            for (var dependencyObject : dependencies) {
                var coordinate = dependencyObject.coordinate();

                Dependency dependency;
                switch (coordinate) {
                    case Project.Coordinate.Maven maven -> {
                        var repos = dependencyObject.repositories()
                                .stream()
                                .map(knownRepositories::get)
                                .toList();
                        if (repos.isEmpty()) {
                            repos = List.of(MavenRepository.central());
                        }
                        dependency = new Dependency(
                                new Library(
                                        new Group(maven.groupId()),
                                        new Artifact(maven.artifactId())
                                ),
                                new MavenCoordinate(
                                        new Group(maven.groupId()),
                                        new Artifact(maven.artifactId()),
                                        new Version(maven.version()),
                                        repos
                                )
                        );
                    }
                    case Project.Coordinate.Path(String path) -> {
                        dependency = Dependency.fromCoordinate(Path.of(path).toString());
                    }
                    case Project.Coordinate.URL(String url) -> {
                        dependency = Dependency.fromCoordinate(url);
                    }
                }

                var usages = dependencyObject.usages().stream()
                        .map(Usage::new)
                        .toList();

                if (usages.isEmpty()) {
                    if (defaultUsage == null) {
                        err.println("Dependency missing \"usage\" and no \"defaultUsage\" specified: " + coordinate);
                        err.flush();
                        return 1;
                    }
                }


                Collection<String> dependencySets = dependencyObject.dependencySets();
                if (dependencySets == null) {
                    dependencySets = List.of("default");
                }

                dependencySets = new LinkedHashSet<>(dependencySets);

                // Doing the tree logic for extends is annoying
                // Doing N passes over the array works too and is less
                // annoying to write, if harder to read and slower and bad
                for (int i = 0; i < dependencySetExtends.size(); i++) {
                    for (var entry : dependencySetExtends.entrySet()) {
                        if (dependencySets.contains(entry.getValue())) {
                            dependencySets.add(entry.getKey());
                        }
                    }
                }

                for (String dependencySet : dependencySets) {
                    dependencySetToDeps.putIfAbsent(dependencySet, new ArrayList<>());
                    dependencySetToDeps.get(dependencySet).add(new UsagesAndDep(usages, dependency));
                }
            }

            if (printTree) {
                for (var entry : dependencySetToDeps.entrySet()) {
                    var dependencySet = entry.getKey();
                    out.println(dependencySet);
                    out.println("-".repeat(50));
                    var usagesAndDeps = entry.getValue();

                    var cache = cachePath == null ? Cache.standard() : Cache.standard(cachePath.toPath());
                    var resolve = new Resolve().withCache(cache);
                    for (var usagesAndDep : usagesAndDeps) {
                        resolve.addDependency(usagesAndDep.dependency);
                    }
                    var resolution = resolve.run();
                    resolution.printTree(out);
                    out.println();
                }

                out.flush();
                return 0;
            }

            for (var entry : dependencySetToDeps.entrySet()) {
                var dependencySet = entry.getKey();

                var libraryToUsages = new LinkedHashMap<Library, Set<Usage>>();

                var usagesAndDeps = entry.getValue();

                var cache = cachePath == null ? Cache.standard() : Cache.standard(cachePath.toPath());
                var resolve = new Resolve().withCache(cache);
                for (var usagesAndDep : usagesAndDeps) {
                    libraryToUsages.put(usagesAndDep.dependency.library(), new LinkedHashSet<>(usagesAndDep.usages));
                    resolve.addDependency(usagesAndDep.dependency);
                }

                var resolution = resolve.run();

                var librariesForUsage = resolution.librariesForUsage(
                        libraryToUsages,
                        defaultUsage
                );

                var fetch = resolution.fetch().run();

                var args = new ArrayList<String>();
                librariesForUsage.forEach(((usage, libraries) -> {
                    if (!libraries.isEmpty()) {
                        var libToPath = new HashMap<>(fetch.libraries());
                        libToPath.keySet().retainAll(libraries);

                        var usageValue = usage.value();
                        var path = libToPath.values()
                                .stream()
                                .map(Path::toString)
                                .collect(Collectors.joining(File.pathSeparator));
                        if (usageValue.endsWith(":") || usageValue.endsWith("=")) {
                            args.add(usageValue + path);
                        } else {
                            args.add(usageValue);
                            args.add(path);
                        }
                    }
                }));

                dependencySetToPath.put(dependencySet, String.join("\n", args));
                Files.createDirectories(dependencySetsPath);
                Files.writeString(dependencySetsPath.resolve(dependencySet), String.join("\n", args));
            }


        }
        var argumentFiles = project.argumentFiles();
        if (argumentFiles != null && !argumentFiles.isEmpty()) {
            for (var argumentFile : argumentFiles) {
                List<String> args;
                try {
                    args = argumentFile.toArgs(dependencySetToPath);
                } catch (RuntimeException e) {
                    err.println(e.getMessage());
                    err.flush();
                    return 1;
                }

                Files.writeString(Path.of(argumentFile.name()), String.join("\n", args));
            }
        }

        List<String> gitignore;
        try {
            gitignore = new ArrayList<>(Files.readAllLines(Path.of(".gitignore")));
        } catch (NoSuchFileException e) {
            gitignore = new ArrayList<>();
        }

        if (!gitignore.contains("dependencySets")) {
            gitignore.add("dependencySets");
        }
        if (argumentFiles != null && !argumentFiles.isEmpty()) {
            for (var argumentFile : argumentFiles) {
                if (!gitignore.contains(argumentFile.name())) {
                    gitignore.add(argumentFile.name());
                }
            }
        }
        Files.writeString(Path.of(".gitignore"), String.join("\n", gitignore));



        /*
        if (enrichPom != null) {
            var pomContents = Files.readString(enrichPom.toPath());
            var factory = DocumentBuilderFactory.newDefaultInstance();
            Document document;
            try {
                document = factory
                        .newDocumentBuilder()
                        .parse(new InputSource(new StringReader(pomContents)));
            } catch (SAXException | IOException e) {
                err.println("Error parsing POM file: " + e.getMessage());
                err.flush();
                return 1;
            }
            var rootElement = document.getDocumentElement();

            Node dependenciesElement = null;
            var rootElementChildren = rootElement.getChildNodes();
            for (int i = 0; i < rootElementChildren.getLength(); i++) {
                var projectElementChild = rootElementChildren.item(i);
                if (projectElementChild.getNodeName().equals("dependencies")) {
                    dependenciesElement = projectElementChild;
                }

            }

            if (dependenciesElement == null) {
                dependenciesElement = document.createElement("dependencies");
                rootElement.appendChild(dependenciesElement);
            }

            var firstChild = dependenciesElement.getFirstChild();
            while (firstChild != null) {
                dependenciesElement.removeChild(firstChild);
                firstChild = dependenciesElement.getFirstChild();
            }

            for (var dependency : resolution.selectedDependencies()) {
                if (dependency.coordinate() instanceof MavenCoordinate mavenCoordinate) {
                    var dependencyElement = document.createElement("dependency");

                    var groupIdElement = document.createElement("groupId");
                    groupIdElement.setTextContent(mavenCoordinate.group().value());
                    dependencyElement.appendChild(groupIdElement);

                    var artifactIdElement = document.createElement("artifactId");
                    artifactIdElement.setTextContent(mavenCoordinate.artifact().value());
                    dependencyElement.appendChild(artifactIdElement);

                    var versionElement = document.createElement("version");
                    versionElement.setTextContent(mavenCoordinate.version().toString());
                    dependencyElement.appendChild(versionElement);

                    dependenciesElement.appendChild(dependencyElement);
                }
            }

            var transformerFactory = TransformerFactory.newDefaultInstance();

            Transformer transformer = transformerFactory.newTransformer(new StreamSource(new ByteArrayInputStream("""
                    <xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
                      <xsl:output indent="yes"/>
                      <xsl:strip-space elements="*"/>
                    
                      <xsl:template match="@*|node()">
                        <xsl:copy>
                          <xsl:apply-templates select="@*|node()"/>
                        </xsl:copy>
                      </xsl:template>
                    
                    </xsl:stylesheet>
                    """.getBytes()
            )));
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            // pretty print

            DOMSource source = new DOMSource(document);
            try (var os = Files.newOutputStream(enrichPom.toPath())) {
                transformer.transform(source, new StreamResult(os));
            }
        }
         */


        return 0;
    }

    @CommandLine.Command(
            name = "enrich-pom",
            description = "Enriches the given pom.xml file with the result of dependency resolution."
    )
    public int enrichPom(
            @CommandLine.Option(
                    names = {"--input", "-i"},
                    description = "The pom.xml file to use as input.",
                    required = true
            )
            File input,

            @CommandLine.Option(
                    names = {"--output", "-o"},
                    description = "The path to output the new pom.xml file.",
                    required = true
            )
            File output,

            @CommandLine.Option(
                    names = {"--dependency-set"},
                    description = "The dependency set to use.",
                    required = true
            )
            String dependencySet
    ) {
        err.println("TODO");
        err.flush();
        return 0;
    }
}