import dev.mccue.project.JProjectToolProvider;

import java.util.spi.ToolProvider;

module dev.mccue.project {
    requires dev.mccue.resolve;
    requires dev.mccue.json;
    requires dev.mccue.purl;
    requires info.picocli;
    requires java.xml;
    requires dev.mccue.guava.graph;

    opens dev.mccue.project
            to info.picocli;

    exports dev.mccue.project;

    provides ToolProvider
            with JProjectToolProvider;
}