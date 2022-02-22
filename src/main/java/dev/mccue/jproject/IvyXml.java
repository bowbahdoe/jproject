package dev.mccue.jproject;

public final class IvyXml {
    public IvyXml() {
    }

    public String contents() {
        // language=xml
        return """
        <ivy-module version="2.0"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:noNamespaceSchemaLocation="http://ant.apache.org/ivy/schemas/ivy.xsd">
            <info module="" organisation=""/>
            <dependencies>
                <dependency org="commons-lang" name="commons-lang" rev="2.0"/>
                <dependency org="commons-cli" name="commons-cli" rev="1.0"/>
            </dependencies>
        </ivy-module>
        """;
    }
}
