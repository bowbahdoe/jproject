package dev.mccue.jproject;

import dev.mccue.jproject.model.ApplicationModule;
import dev.mccue.jproject.model.DependencyScope;
import dev.mccue.jproject.model.MavenDependency;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class IvyXml {
    private IvyXml() {}

    public static String contents(
            ApplicationModule applicationModule
    ) {
        var deps = applicationModule.deps();
        System.out.println(deps);
        var defaultDeps = deps.entrySet().stream()
                .map(entry -> {
                    var dep = entry.getKey();
                    var scopes = entry.getValue();
                    return "<dependency org=\"%s\" name=\"%s\" rev=\"%s\" conf=\"%s\"/>".formatted(
                            dep.coordinate().groupId(),
                            dep.coordinate().artifactId(),
                            dep.version(),
                            scopes.stream()
                                    .map(scope ->
                                        switch (scope) {
                                            case ALL -> "default";
                                            case TEST -> "test->default";
                                            case COMPILE -> "compile->default";
                                            case RUNTIME -> "runtime->default";
                                        }
                                    )
                                    .collect(Collectors.joining(";"))
                    );
                })
                .collect(Collectors.joining("\n        "));

        // language=xml
        return """
        <ivy-module version="2.0"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:noNamespaceSchemaLocation="http://ant.apache.org/ivy/schemas/ivy.xsd">
            <info module="" organisation=""/>
             <configurations defaultconfmapping="default">
                <conf name="compile" visibility="private"/>
                <conf name="test" extends="compile" visibility="private"/>
                <conf name="master" />
                <conf name="runtime" extends="compile"/>
                <conf name="default" extends="master,runtime"/>
             </configurations>
            <dependencies>
                %s
            </dependencies>
        </ivy-module>
        """.formatted(defaultDeps);
    }
}
