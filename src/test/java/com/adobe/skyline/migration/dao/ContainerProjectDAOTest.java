/*
 Copyright 2020 Adobe. All rights reserved.
 This file is licensed to you under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software distributed under
 the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 OF ANY KIND, either express or implied. See the License for the specific language
 governing permissions and limitations under the License.
 */

package com.adobe.skyline.migration.dao;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.adobe.skyline.migration.MigrationConstants;
import com.adobe.skyline.migration.SkylineMigrationBaseTest;
import com.adobe.skyline.migration.testutils.TestConstants;
import com.adobe.skyline.migration.util.XmlUtil;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ContainerProjectDAOTest extends SkylineMigrationBaseTest {

    private XPath xPath;

    @Before
    public void setUp(){
        super.setUp();
        this.xPath = XPathFactory.newInstance().newXPath();
    }

    @Test
    public void testNewProjectsAddedToAllProjectWhenPresent() throws ParserConfigurationException, SAXException, IOException {
        File tempProjectRoot = projectLoader.copyMigratedProjectToTemp(temp);
        ContainerProjectDAO dao = new ContainerProjectDAO(Paths.get(tempProjectRoot.getAbsolutePath(),
                TestConstants.CONTAINER_PACKAGE_PROJECT_NAME).toString());

        dao.addProject(TestConstants.TEST_PROJECT_GROUP_ID, MigrationConstants.MIGRATION_PROJECT_CONTENT);
        dao.addProject(TestConstants.TEST_PROJECT_GROUP_ID, MigrationConstants.MIGRATION_PROJECT_APPS);

        File allPom = new File(Paths.get(tempProjectRoot.getPath(), TestConstants.CONTAINER_PACKAGE_PROJECT_NAME,
                MigrationConstants.POM_XML).toString());
        Document doc = XmlUtil.loadXml(allPom);

        assertArtifactInEmbeddeds(MigrationConstants.MIGRATION_PROJECT_CONTENT, doc);
        assertArtifactInEmbeddeds(MigrationConstants.MIGRATION_PROJECT_APPS, doc);
        assertArtifactInDependencies(MigrationConstants.MIGRATION_PROJECT_CONTENT, doc);
        assertArtifactInDependencies(MigrationConstants.MIGRATION_PROJECT_APPS, doc);
    }

    @Test
    public void testFilterAdded() throws ParserConfigurationException, SAXException, IOException {
        File tempProjectRoot = projectLoader.copyMigratedProjectToTemp(temp);
        ContainerProjectDAO dao = new ContainerProjectDAO(Paths.get(tempProjectRoot.getAbsolutePath(),
                TestConstants.CONTAINER_PACKAGE_PROJECT_NAME).toString());

        dao.addProject(TestConstants.TEST_PROJECT_GROUP_ID, MigrationConstants.MIGRATION_PROJECT_CONTENT);

        File filterFile = new File(Paths.get(tempProjectRoot.getPath(), TestConstants.CONTAINER_PACKAGE_PROJECT_NAME,
                MigrationConstants.PATH_TO_FILTER_XML).toString());

        Document filterXml = XmlUtil.loadXml(filterFile);
        Element workspaceFilterElement = filterXml.getDocumentElement();
        List<Node> filterNodes = XmlUtil.getChildElementNodes(workspaceFilterElement);

        boolean matched = false;
        for (Node currNode : filterNodes) {
            String path = ((Element) currNode).getAttribute(MigrationConstants.ROOT_PROPERTY);
            if (MigrationConstants.MIGRATION_PACKAGE_PATH.equals(path)) {
                matched = true;
                break;
            }
        }

        assertTrue(matched);
    }

    private void assertArtifactInEmbeddeds(String artifactId, Document doc) {
        assertNodeOfTypeExistsWithArtifactId(doc, "embedded", artifactId);
    }

    private void assertArtifactInDependencies(String artifactId, Document doc) {
        assertNodeOfTypeExistsWithArtifactId(doc, "dependency", artifactId);
    }

    private void assertNodeOfTypeExistsWithArtifactId(Document doc, String nodeType, String artifactId) {
        try {
            String expression = "//"+ nodeType + "[descendant::artifactId[text()=" + "'" + artifactId + "'" + "]]";
            NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
            assertTrue(nodeList.getLength() > 0);
        } catch (Exception e) {
            fail("Exception caught while searching XML file for " + nodeType + " with a value of " + artifactId + ".");
            e.printStackTrace();
        }
    }

}