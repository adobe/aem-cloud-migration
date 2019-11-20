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

package com.adobe.skyline.migration.dao;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.adobe.skyline.migration.MigrationConstants;
import com.adobe.skyline.migration.SkylineMigrationBaseTest;
import com.adobe.skyline.migration.exception.CustomerDataException;
import com.adobe.skyline.migration.exception.ProjectCreationException;
import com.adobe.skyline.migration.model.ChangeTrackingService;
import com.adobe.skyline.migration.testutils.TestConstants;
import com.adobe.skyline.migration.util.XmlUtil;

public class MavenProjectDAOTest extends SkylineMigrationBaseTest {

    private File tempProjectRoot;
    private ChangeTrackingService changeTracker;

    private MavenProjectDAO creator;

    @Before
    public void setUp(){
        super.setUp();

        this.tempProjectRoot = projectLoader.copyConfProjectToTemp(temp);
        this.changeTracker = new ChangeTrackingService();

        try {
            this.creator = new MavenProjectDAO(this.tempProjectRoot.getAbsolutePath(), changeTracker);
        } catch (CustomerDataException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testTemplateProjectsCopied() throws ProjectCreationException, ParserConfigurationException, SAXException, IOException {
        creator.createProject(MigrationConstants.MIGRATION_PROJECT_CONTENT);
        creator.createProject(MigrationConstants.MIGRATION_PROJECT_APPS);

        assertProjectCreated(MigrationConstants.MIGRATION_PROJECT_CONTENT);
        assertProjectCreated(MigrationConstants.MIGRATION_PROJECT_APPS);
    }

    @Test
    public void testNewProjectsAddedToReactor() throws ProjectCreationException, ParserConfigurationException, SAXException, IOException {
        creator.createProject(MigrationConstants.MIGRATION_PROJECT_CONTENT);
        creator.createProject(MigrationConstants.MIGRATION_PROJECT_APPS);

        File reactorPom = new File(tempProjectRoot, MigrationConstants.POM_XML);
        Document pomDoc = XmlUtil.loadXml(reactorPom);
        NodeList moduleNodes = pomDoc.getElementsByTagName(MigrationConstants.MODULE_TAG_NAME);

        boolean contentMatched = false;
        boolean appsMatched = false;
        for (int i  = 0; i < moduleNodes.getLength(); i++) {
            Node moduleNode = moduleNodes.item(i);
            if (MigrationConstants.MIGRATION_PROJECT_CONTENT.equals(moduleNode.getTextContent())) {
                contentMatched = true;
            } else if (MigrationConstants.MIGRATION_PROJECT_APPS.equals(moduleNode.getTextContent())) {
                appsMatched = true;
            }
        }

        assertTrue(contentMatched && appsMatched);
    }

    @Test
    public void testChangesTracked() throws ProjectCreationException {
        creator.createProject(MigrationConstants.MIGRATION_PROJECT_CONTENT);
        creator.createProject(MigrationConstants.MIGRATION_PROJECT_APPS);

        List<String> projectsCreated = changeTracker.getProjectsCreated();
        assertEquals(2, projectsCreated.size());
        assertTrue(projectsCreated.contains(MigrationConstants.MIGRATION_PROJECT_CONTENT));
        assertTrue(projectsCreated.contains(MigrationConstants.MIGRATION_PROJECT_APPS));
    }

    @Test
    public void testProjectExistsCheck() throws ProjectCreationException {
        assertFalse(creator.projectExists(MigrationConstants.MIGRATION_PROJECT_CONTENT));
        assertFalse(creator.projectExists(MigrationConstants.MIGRATION_PROJECT_APPS));

        creator.createProject(MigrationConstants.MIGRATION_PROJECT_CONTENT);
        creator.createProject(MigrationConstants.MIGRATION_PROJECT_APPS);

        assertTrue(creator.projectExists(MigrationConstants.MIGRATION_PROJECT_CONTENT));
        assertTrue(creator.projectExists(MigrationConstants.MIGRATION_PROJECT_APPS));
    }

    private void assertProjectCreated(String projectName) throws IOException, SAXException, ParserConfigurationException {
        File project = new File(tempProjectRoot, projectName);
        assertTrue(project.exists());
        assertParentPropertiesSet(project);
    }

    private void assertParentPropertiesSet(File project) throws ParserConfigurationException, SAXException, IOException {
        File pomFile = new File(project, MigrationConstants.POM_XML);
        Document pom = XmlUtil.loadXml(pomFile);

        NodeList nodes = pom.getElementsByTagName(MigrationConstants.PARENT_TAG_NAME);
        NodeList parentPropNodes = nodes.item(0).getChildNodes();

        boolean groupIdMatched = false;
        boolean artifactIdMatched = false;
        boolean versionMatched = false;
        boolean relativePathMatched = false;

        for (int i = 0; i < parentPropNodes.getLength(); i++) {
            Node node = parentPropNodes.item(i);

            if (node.getNodeName().equals(MigrationConstants.GROUPID_TAG_NAME)) {
                groupIdMatched = true;
                assertEquals(TestConstants.TEST_PROJECT_GROUP_ID, node.getTextContent());
            } else if (node.getNodeName().equals(MigrationConstants.ARTIFACTID_TAG_NAME)) {
                artifactIdMatched = true;
                assertEquals(TestConstants.TEST_PROJECT_ARTIFACT_ID, node.getTextContent());
            } else if (node.getNodeName().equals(MigrationConstants.VERSION_TAG_NAME)) {
                versionMatched = true;
                assertEquals(TestConstants.TEST_PROJECT_VERSION, node.getTextContent());
            } else if (node.getNodeName().equals(MigrationConstants.RELATIVEPATH_TAG_NAME)) {
                relativePathMatched = true;
                assertEquals("../pom.xml", node.getTextContent());
            }
        }

        assertTrue(groupIdMatched);
        assertTrue(artifactIdMatched);
        assertTrue(versionMatched);
        assertTrue(relativePathMatched);
    }

}