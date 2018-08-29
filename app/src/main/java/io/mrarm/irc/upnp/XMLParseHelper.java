package io.mrarm.irc.upnp;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class XMLParseHelper {

    public static Element findChildElement(Element element, String name) {
        if (element == null)
            return null;
        for (int i = element.getChildNodes().getLength() - 1; i >= 0; --i) {
            Node child = element.getChildNodes().item(i);
            if (child instanceof Element && child.getLocalName().equalsIgnoreCase(name))
                return ((Element) child);
        }
        return null;
    }

    public static String getChildElementValue(Element element, String name, String def) {
        Element ret = findChildElement(element, name);
        if (ret == null)
            return def;
        return ret.getTextContent() != null ? ret.getTextContent() : def;
    }

}
