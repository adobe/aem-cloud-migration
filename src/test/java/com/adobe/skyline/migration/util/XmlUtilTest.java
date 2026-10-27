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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class XmlUtilTest {

    private static final String ORIGINAL = String.join("\n",
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
            "<!-- Licensed to the customer -->",
            "<jcr:root xmlns:jcr=\"http://www.jcp.org/jcr/1.0\" xmlns:cq=\"http://www.day.com/jcr/cq/1.0\"",
            "    jcr:primaryType=\"cq:WorkflowLauncher\"",
            "    enabled=\"{Boolean}true\"",
            "    conditions=\"[jcr:content/jcr:mimeType!=video/.*]\"",
            "    script=\"var a = 1;&#xd;&#xa;return a;\">",
            "    <!-- the steps -->",
            "    <first",
            "        jcr:primaryType=\"nt:unstructured\"/>",
            "    <second",
            "        jcr:primaryType=\"nt:unstructured\"/>",
            "</jcr:root>",
            "");

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void testChangedAttributeOnlyEditsThatAttribute() throws Exception {
        File file = write(ORIGINAL);
        Document doc = XmlUtil.loadXml(file);
        doc.getDocumentElement().setAttribute("enabled", "{Boolean}false");

        XmlUtil.writeXml(doc, file);

        assertEquals(ORIGINAL.replace("enabled=\"{Boolean}true\"", "enabled=\"{Boolean}false\""), read(file));
    }

    @Test
    public void testRemovedElementOnlyRemovesItsLines() throws Exception {
        File file = write(ORIGINAL);
        Document doc = XmlUtil.loadXml(file);
        Element root = doc.getDocumentElement();
        root.removeChild(root.getElementsByTagName("first").item(0));

        XmlUtil.writeXml(doc, file);

        assertEquals(ORIGINAL.replace("\n    <first\n        jcr:primaryType=\"nt:unstructured\"/>", ""), read(file));
    }

    @Test
    public void testAddedElementIndentedLikeItsSiblings() throws Exception {
        File file = write(ORIGINAL);
        Document doc = XmlUtil.loadXml(file);
        Element third = doc.createElement("third");
        third.setAttribute("jcr:primaryType", "nt:unstructured");
        doc.getDocumentElement().appendChild(third);

        XmlUtil.writeXml(doc, file);

        assertEquals(ORIGINAL.replace("</jcr:root>", "    <third jcr:primaryType=\"nt:unstructured\"/>\n</jcr:root>"), read(file));
    }

    @Test
    public void testMovedElementKeepsItsFormatting() throws Exception {
        File file = write(ORIGINAL);
        Document doc = XmlUtil.loadXml(file);
        Element root = doc.getDocumentElement();
        Node first = root.getElementsByTagName("first").item(0);
        root.removeChild(first);
        root.appendChild(first);

        XmlUtil.writeXml(doc, file);

        String result = read(file);
        assertTrue(result, result.contains("<!-- Licensed to the customer -->"));
        assertTrue(result, result.contains("script=\"var a = 1;&#xd;&#xa;return a;\""));
        assertTrue(result, result.indexOf("<second") < result.indexOf("<first"));
        assertEquals(ORIGINAL.replace("\n    <first\n        jcr:primaryType=\"nt:unstructured\"/>", "")
                .replace("</jcr:root>", "    <first\n        jcr:primaryType=\"nt:unstructured\"/>\n</jcr:root>"), result);
    }

    @Test
    public void testNewFileWritten() throws Exception {
        Document doc = XmlUtil.createXml();
        Element root = doc.createElement("workspaceFilter");
        root.setAttribute("version", "1.0");
        doc.appendChild(root);
        File file = new File(temp.getRoot(), "filter.xml");

        XmlUtil.writeXml(doc, file);

        assertTrue(read(file).contains("<workspaceFilter version=\"1.0\"/>"));
    }

    private File write(String content) throws Exception {
        File file = temp.newFile("test.xml");
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private String read(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
}
