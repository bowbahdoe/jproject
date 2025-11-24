package dev.mccue.project;

import javax.xml.XMLConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.SequencedCollection;
import java.util.function.Predicate;

interface XMLDecoder<T> {
    T decode(XML xml);

    static void assertOnlyNodesNamed(XML.Node xml, SequencedCollection<String> names) {
        for (var n : xml.children()) {
            if (n instanceof XML.Node nn) {
                if (!names.contains(nn.name())) {
                    throw new XMLDecodeException(
                            "Unsupported top level element: "
                                    + nn.name()
                                    + ", supported: "
                                    + names
                    );
                }
            }
        }
    }

    static XML.Node node(XML xml) {
        if (xml instanceof XML.Node node) {
            return node;
        }
        else {
            throw new XMLDecodeException("Expected Node: " + xml);
        }
    }
    static XML.Text text(XML xml) {
        if (xml instanceof XML.Text text) {
            return text;
        }
        else {
            throw new XMLDecodeException("Expected text: " + xml);
        }
    }

    private static boolean isNamed(XML xml, String name) {
        return xml instanceof XML.Node node && name.equals(node.name());
    }

    static String nodeName(XML xml) {
        if (xml instanceof XML.Node node) {
            return node.name();
        }
        else {
            throw new XMLDecodeException("Expected an element with a name: " + xml);
        }
    }

    static List<XML> allChildrenWhere(XML xml, Predicate<XML> test) {
        return allChildrenWhere(xml, test, a -> a);
    }

    static List<XML> allChildrenNamed(XML xml, String name) {
        return allChildrenWhere(xml, element -> isNamed(element, name));
    }

    static <T> List<T> allChildrenWhere(XML xml, Predicate<XML> test, XMLDecoder<T> decoder) {
        if (xml instanceof XML.Node node) {
            var children = node.children();
            var results = new ArrayList<T>();
            for (var child : children) {
                if (test.test(child)) {
                    results.add(decoder.decode(child));
                }
            }
            return results;
        }
        else {
            throw new XMLDecodeException("Expected node: " + xml);
        }
    }

    static <T> T onlyChildWhere(XML xml, Predicate<XML> test, XMLDecoder<T> decoder) {
        var result = singleChildWhere(xml, test, decoder);
        if (result == null) {
            throw new XMLDecodeException("No children found");
        }
        return result;
    }

    static String textValue(XML xml) {
        return "";
    }

    static XML singleChildWhere(XML xml, Predicate<XML> test) {
        var children = allChildrenWhere(xml, test);
        if (children.isEmpty()) {
            return null;
        }
        else if (children.size() > 1) {
            throw new XMLDecodeException("Too many children");
        }
        else {
            return children.getFirst();
        }
    }

    static XML singleChildNamed(XML xml, String name) {
        return singleChildWhere(
                xml,
                element -> isNamed(element, name)
        );
    }

    static <T> T singleChildWhere(XML xml, Predicate<XML> test, XMLDecoder<T> decoder) {
        return decoder.decode(singleChildWhere(xml, test));
    }

    static <T> T singleChildNamed(XML xml, String name, XMLDecoder<T> decoder) {
        var result = singleChildNamed(xml, name);
        if (result == null) {
            return null;
        }
        return decoder.decode(result);
    }

    static String singleTextChild(XML xml) {
        if (!(xml instanceof XML.Node node)) {
            throw new XMLDecodeException("Expected node");
        }

        var children = node.children();
        if (children.isEmpty()) {
            return "";
        }
        else if (children.size() > 1) {
            throw new XMLDecodeException("More than one child");
        }
        else if (!(children.getFirst() instanceof XML.Text text)) {
            throw new XMLDecodeException("First child is not text");
        }
        else {
            return text.value();
        }
    }

    static String singleTextChildStripped(XML xml) {
        return singleTextChild(xml).strip();
    }

    static XML.Node nodeStripBlankTextChildren(XML xml) {
        var node = XMLDecoder.node(xml);
        var newChildren = new ArrayList<XML>();
        for (var child : node.children()) {
            switch (child) {
                case XML.Node __ -> {
                    newChildren.add(child);
                }
                case XML.Text(var value) -> {
                    if (!value.isBlank()) {
                        throw new XMLDecodeException("Text value is not blank: " + child);
                    }
                }
            }
        }

        return new XML.Node(
                node.name(),
                node.attributes(),
                newChildren
        );
    }
}
