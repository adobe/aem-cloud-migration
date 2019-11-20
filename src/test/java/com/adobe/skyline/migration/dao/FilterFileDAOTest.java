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

import com.adobe.skyline.migration.MigrationConstants;
import com.adobe.skyline.migration.SkylineMigrationBaseTest;
import com.adobe.skyline.migration.testutils.TestConstants;
import com.adobe.skyline.migration.util.XmlUtil;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

public class FilterFileDAOTest extends SkylineMigrationBaseTest {

    private File tempProjectRoot;
    private FilterFileDAO dao;

    @Before
    public void setUp() {
        super.setUp();

        this.tempProjectRoot = projectLoader.copyConfProjectToTemp(temp);

        this.dao = new FilterFileDAO(tempProjectRoot + File.separator + TestConstants.WORKFLOW_PROJECT_NAME);
    }

    @Test
    public void testFilterAdded() throws ParserConfigurationException, SAXException, IOException {
        dao.addPath(MigrationConstants.WORKFLOW_RUNNER_CONFIG_PATH);

        File filterFile = new File(tempProjectRoot + File.separator + TestConstants.WORKFLOW_PROJECT_NAME + MigrationConstants.PATH_TO_FILTER_XML);

        Document filterXml = XmlUtil.loadXml(filterFile);
        Element workspaceFilterElement = filterXml.getDocumentElement();
        List<Node> filterNodes = XmlUtil.getChildElementNodes(workspaceFilterElement);

        boolean matched = false;
        for (Node currNode : filterNodes) {
            String path = ((Element) currNode).getAttribute(MigrationConstants.ROOT_PROPERTY);
            if (MigrationConstants.WORKFLOW_RUNNER_CONFIG_PATH.equals(path)) {
                matched = true;
                break;
            }
        }

        assertTrue(matched);
    }

    @Test
    public void testDuplicateFiltersNotAdded() throws ParserConfigurationException, SAXException, IOException {
        dao.addPath(MigrationConstants.WORKFLOW_RUNNER_CONFIG_PATH);
        dao.addPath(MigrationConstants.WORKFLOW_RUNNER_CONFIG_PATH);

        File filterFile = new File(tempProjectRoot + File.separator + TestConstants.WORKFLOW_PROJECT_NAME + MigrationConstants.PATH_TO_FILTER_XML);

        Document filterXml = XmlUtil.loadXml(filterFile);
        Element workspaceFilterElement = filterXml.getDocumentElement();
        List<Node> filterNodes = XmlUtil.getChildElementNodes(workspaceFilterElement);

        int matched = 0;
        for (Node currNode : filterNodes) {
            String path = ((Element) currNode).getAttribute(MigrationConstants.ROOT_PROPERTY);
            if (MigrationConstants.WORKFLOW_RUNNER_CONFIG_PATH.equals(path)) {
                matched++;
            }
        }

        assertEquals(1, matched);
    }
}