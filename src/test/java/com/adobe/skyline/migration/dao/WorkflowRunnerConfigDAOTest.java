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
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.*;

public class WorkflowRunnerConfigDAOTest extends SkylineMigrationBaseTest {

    private static final String CONFIG_FILE_REL_PATH = Paths.get("", MigrationConstants.MIGRATION_PROJECT_APPS, MigrationConstants.PATH_TO_JCR_ROOT,
            MigrationConstants.WORKFLOW_RUNNER_CONFIG_PATH, MigrationConstants.WORKFLOW_RUNNER_CONFIG_FILENAME).toString();

    private static final String PATTERN = "\\/content\\/dam(\\/.*\\/)(marketing\\/seasonal)(\\/.*)";
    private static final String PATTERN_2 = "\\/content\\/dam(\\/.*\\/)(other_path)(\\/.*)";
    private static final String PATTERN_MODEL = "/var/workflow/models/request_for_activation";

    private static final String PATH = "/content/dam";
    private static final String PATH_2 = "/content/dam/subfolder";
    private static final String PATH_MODEL = "/var/workflow/models/dam/dam-autotag-assets";

    private File tempProjectRoot;
    private WorkflowRunnerConfigDAO dao;

    @Before
    public void setUp() {
        super.setUp();

        this.tempProjectRoot = projectLoader.copyConfProjectToTemp(temp);

        this.dao = new WorkflowRunnerConfigDAO(Paths.get(tempProjectRoot.getPath(), MigrationConstants.MIGRATION_PROJECT_APPS).toString());
    }

    @Test
    public void testConfigFileCreated() throws IOException, ParserConfigurationException, SAXException {
        File configFile = new File(Paths.get(tempProjectRoot.getPath(), CONFIG_FILE_REL_PATH).toString());
        assertFalse(configFile.exists());

        dao.createConfigByExpression("", "");

        //Validate required properties
        Document xmlDoc = XmlUtil.loadXml(configFile);
        Element rootElem = xmlDoc.getDocumentElement();
        assertEquals(MigrationConstants.NS_SLING_VALUE, rootElem.getAttribute(MigrationConstants.NS_SLING));
        assertEquals(MigrationConstants.NS_JCR_VALUE, rootElem.getAttribute(MigrationConstants.NS_JCR));
        assertEquals(MigrationConstants.OSGI_CONFIG_TYPE_VALUE, rootElem.getAttribute(MigrationConstants.JCR_PRIMARY_TYPE_PROP));
    }

    @Test
    public void testCreateConfigByExpression() throws IOException, ParserConfigurationException, SAXException {
        dao.createConfigByExpression(PATTERN, PATTERN_MODEL);
        dao.createConfigByExpression(PATTERN_2, PATTERN_MODEL);

        File configFile = new File(Paths.get(tempProjectRoot.getPath(), CONFIG_FILE_REL_PATH).toString());
        List<String> patternMappings = getPatternMappingsFromFile(configFile, MigrationConstants.WORKFLOW_RUNNER_CONFIG_BY_EXPRESSION);

        assertEquals(PATTERN + ":" + PATTERN_MODEL, patternMappings.get(0));
        assertEquals(PATTERN_2 + ":" + PATTERN_MODEL, patternMappings.get(1));
    }

    @Test
    public void testCreateConfigByPath() throws IOException, ParserConfigurationException, SAXException {
        dao.createConfigByPath(PATH, PATH_MODEL);
        dao.createConfigByPath(PATH_2, PATH_MODEL);

        File configFile = new File(Paths.get(tempProjectRoot.getPath(), CONFIG_FILE_REL_PATH).toString());
        List<String> patternMappings = getPatternMappingsFromFile(configFile, MigrationConstants.WORKFLOW_RUNNER_CONFIG_BY_PATH);

        assertEquals(PATH + ":" + PATH_MODEL, patternMappings.get(0));
        assertEquals(PATH_2 + ":" + PATH_MODEL, patternMappings.get(1));
    }

    private List<String> getPatternMappingsFromFile(File configFile, String mappingProp) throws ParserConfigurationException, SAXException, IOException {
        Document xmlDoc = XmlUtil.loadXml(configFile);
        Element rootElem = xmlDoc.getDocumentElement();
        return XmlUtil.getStringArrayListFromAttribute(rootElem, mappingProp);
    }

}