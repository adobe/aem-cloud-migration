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

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.adobe.skyline.migration.MigrationConstants;
import com.adobe.skyline.migration.SkylineMigrationBaseTest;
import com.adobe.skyline.migration.testutils.TestConstants;
import com.adobe.skyline.migration.util.XmlUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FilterFileDAOTest extends SkylineMigrationBaseTest {

    private File tempProjectRoot;
    private File filterFile;

    private FilterFileDAO dao;

    @Before
    public void setUp() {
        super.setUp();

        this.tempProjectRoot = projectLoader.copyConfProjectToTemp(temp);
        this.filterFile = new File(Paths.get(tempProjectRoot.getPath(), TestConstants.CONF_WORKFLOW_PROJECT_NAME, MigrationConstants.PATH_TO_FILTER_XML).toString());

        this.dao = new FilterFileDAO(Paths.get(tempProjectRoot.getPath(), TestConstants.CONF_WORKFLOW_PROJECT_NAME).toString());
    }

    @Test
    public void testFilterAdded() throws ParserConfigurationException, SAXException, IOException {
        dao.addPath(MigrationConstants.WORKFLOW_RUNNER_CONFIG_PATH);

        assertEquals(1, findPathMatches(MigrationConstants.WORKFLOW_RUNNER_CONFIG_PATH));
    }

    @Test
    public void testDuplicateFiltersNotAdded() throws ParserConfigurationException, SAXException, IOException {
        dao.addPath(MigrationConstants.WORKFLOW_RUNNER_CONFIG_PATH);
        dao.addPath(MigrationConstants.WORKFLOW_RUNNER_CONFIG_PATH);

        assertEquals(1, findPathMatches(MigrationConstants.WORKFLOW_RUNNER_CONFIG_PATH));
    }

    @Test
    public void testFindPaths() {
        Pattern confPattern = Pattern.compile("^/conf.*");

        List<String> pathsFound = dao.findPathsWith(confPattern);

        assertTrue(pathsFound.contains("/conf/global/settings/workflow"));
        assertTrue(pathsFound.contains("/conf/sample"));
    }

    @Test
    public void testRemovePath() throws IOException, SAXException, ParserConfigurationException {
        assertEquals(1, findPathMatches("/conf/sample"));

        dao.removePath("/conf/sample");

        assertEquals(0, findPathMatches("/conf/sample"));
    }

    private int findPathMatches(String path) throws ParserConfigurationException, SAXException, IOException {
        Document filterXml = XmlUtil.loadXml(filterFile);
        Element workspaceFilterElement = filterXml.getDocumentElement();
        List<Node> filterNodes = XmlUtil.getChildElementNodes(workspaceFilterElement);

        int matched = 0;
        for (Node currNode : filterNodes) {
            String xmlPath = ((Element) currNode).getAttribute(MigrationConstants.ROOT_PROPERTY);
            if (path.equals(xmlPath)) {
                matched++;
            }
        }

        return matched;
    }
}