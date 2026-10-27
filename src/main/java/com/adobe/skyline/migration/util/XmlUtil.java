/*
 Copyright 2019 Adobe. All rights reserved.
 This file is licensed to you under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software distributed under
 the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 OF ANY KIND, either express or implied. See the License for the specific language
 governing permissions and limitations under the License.
 */

package com.adobe.skyline.migration.util;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

/**
 * Utilities for interacting with XML files
 */
public class XmlUtil {

    public static Document createXml() throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.newDocument();
    }

    public static Document loadXml(File xmlFile) throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setIgnoringComments(true);
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(xmlFile);
        doc.getDocumentElement().normalize();
        return doc;
    }

    /**
     * Writes the document. If the file already exists, only the parts that changed are rewritten so the formatting,
     * comments and attribute order of the rest of the file are kept.
     */
    public static void writeXml(Document doc, File target) throws TransformerException, IOException {
        String serialized = serialize(doc);
        String content = serialized;
        if (target.exists()) {
            try {
                content = XmlTextDiff.minimize(Files.readString(target.toPath(), StandardCharsets.UTF_8), serialized);
            } catch (CharacterCodingException e) {
                content = serialized;
            }
        }
        Files.writeString(target.toPath(), content, StandardCharsets.UTF_8);
    }

    private static String serialize(Document doc) throws TransformerException {
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));

        StringBuilder content = new StringBuilder();
        for (String line : writer.toString().split("\\r?\\n")) {
            if (!line.trim().isEmpty()) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    public static List<Node> getChildElementNodes(Node parentNode) {
        List<Node> childElements = new ArrayList<>();

        NodeList children = parentNode.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node currNode = children.item(i);
            if (currNode.getNodeType() == Node.ELEMENT_NODE) {
                childElements.add(currNode);
            }
        }

        return childElements;
    }

    /**
     * @return the value of the attribute, or null if the node has no such attribute
     */
    public static String getAttribute(Node node, String attr) {
        if (node.getAttributes() == null) {
            return null;
        }
        Node attrNode = node.getAttributes().getNamedItem(attr);
        return attrNode == null ? null : attrNode.getNodeValue();
    }

    public static Attr getOrCreateAttr(Element el, String attr) {
        if (el.getAttribute(attr).length() > 0) { //We already have existing values.
            return el.getAttributeNode(attr);
        } else { //We don't have existing values.  Create.
            Attr newAttr = el.getOwnerDocument().createAttribute(attr);
            el.setAttributeNode(newAttr);
            return newAttr;
        }
    }

    public static List<String> getStringArrayListFromAttribute(Element el, String att) {
        String attVal = el.getAttribute(att);
        return StringUtil.getListFromString(attVal);
    }

    public static String getSerializedArrayValueFromList(List<String> list) {
        return "[" + StringUtil.concatenateCollectionToCsv(list) + "]";
    }
}
