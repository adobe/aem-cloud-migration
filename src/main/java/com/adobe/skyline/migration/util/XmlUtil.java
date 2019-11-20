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

import com.adobe.skyline.migration.util.file.FileUtil;
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

    public static void writeXml(Document doc, File target) throws TransformerException, IOException {
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(target);
        transformer.transform(source, result);
        FileUtil.removeEmptyLinesFromFile(target);
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
