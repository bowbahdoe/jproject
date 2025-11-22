package dev.mccue.project;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

sealed interface XML {
    record Node(String name,
                SequencedMap<String, String> attributes,
                List<XML> children
    ) implements XML {}
    record Text(String value) implements XML {}

    static XML from(Document document) {
        var rootElement = document.getDocumentElement();
        return from(rootElement);
    }

    static XML from(Element element) {
        if (element.getNodeType() == Element.TEXT_NODE) {
            return new Text(element.getNodeValue());
        } else if (element.getNodeType() == Element.ELEMENT_NODE) {
            var children = new ArrayList<XML>();
            var childNodes = element.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                var childNode = childNodes.item(i);
                if (childNode instanceof Element) {
                    var child = from((Element) childNode);
                    if (child != null) {
                        children.add(child);
                    }
                }
                else if (childNode instanceof org.w3c.dom.Text textNode) {
                    children.add(new Text(textNode.getNodeValue()));
                }

            }
            return new Node(
                    element.getTagName(),
                    new LinkedHashMap<>(),
                    children
            );
        }
        else {
            return null;
        }
    }
}
