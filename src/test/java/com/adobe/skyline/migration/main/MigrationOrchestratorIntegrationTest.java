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

package com.adobe.skyline.migration.main;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import javax.xml.parsers.ParserConfigurationException;

import com.adobe.skyline.migration.exception.ProjectCreationException;
import com.adobe.skyline.migration.util.XmlUtil;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.adobe.skyline.migration.MigrationConstants;
import com.adobe.skyline.migration.SkylineMigrationBaseTest;
import com.adobe.skyline.migration.exception.CustomerDataException;
import com.adobe.skyline.migration.testutils.TestConstants;

@SuppressWarnings("SameParameterValue")
public class MigrationOrchestratorIntegrationTest extends SkylineMigrationBaseTest {

    private File reportOutputDir;

    @Before
    public void setUp() {
        try {
            super.setUp();

            reportOutputDir = temp.newFolder();
        } catch(Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testMigrationMigratedProject() {
        File testProject  = projectLoader.copyMigratedProjectToTemp(temp);
        testConfBasedProject(testProject);
    }

    @Test
    public void testMigrationConfPath() {
        File testProject  = projectLoader.copyConfProjectToTemp(temp);
        testConfBasedProject(testProject);
    }

    private void testConfBasedProject(File testProject) {
        try {
            new MigrationOrchestrator(testProject.getPath(), config, reportOutputDir.getPath()).exec();

            assertLauncherEnabled(getAbsolutePathForConfLauncher(testProject, "global-marketing-create-project-launcher"));

            String[] launchersToDisable = new String[]{
                    "scene7_digitalfolder",
                    "scene7_digitalfolder_delivery_shop",
                    "scene7_digitalfolder_delivery_shop_node_modified_launcher",
                    "scene7_digitalfolder_delivery_site_node_modified_launcher",
                    "update-asset-marketing-launcher",
                    "update-asset-marketing-node-modified-launcher",
                    "update_asset_create","update_asset_mod"
            };

            for (String launcher : launchersToDisable) {
                assertLauncherDisabled(getAbsolutePathForConfLauncher(testProject, launcher));
            }

            String[] damUpdateAssetStepsToRemove = new String[] {
                    "com.day.cq.dam.core.process.GateKeeperProcess",
                    "com.day.cq.dam.core.process.MetadataProcessorProcess",
                    "com.day.cq.dam.video.FFMpegThumbnailProcess",
                    "com.day.cq.dam.core.process.CommandLineProcess",
                    "com.day.cq.dam.core.process.CreatePdfPreviewProcess",
                    "com.day.cq.dam.core.process.ThumbnailProcess"
            };

            assertStepsRemovedFromWorkflowModel(damUpdateAssetStepsToRemove, getAbsolutePathForConfModel(testProject, "dam/update_asset"));
            assertWorkflowRunnerConfigExistsForModel(testProject, "dam/update_asset");

            List<String> expectedProfiles = Arrays.asList("migrated_from_update_asset", "migrated_from_update_asset_marketing");

            assertProcessingProfilesCreated(testProject, expectedProfiles);
            assertBothNewModulesAddedToReactor(testProject);
            validateConfProjectReportOutput();
        } catch (Exception e) {
            fail("An exception occurred during testing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testMigrationLegacyPath() throws CustomerDataException, ProjectCreationException, IOException, SAXException, ParserConfigurationException {

        File testProject  = projectLoader.copyEtcProjectToTemp(temp);
        new MigrationOrchestrator(testProject.getPath(), config, reportOutputDir.getPath()).exec();

        assertLauncherEnabled(getAbsolutePathForEtcLauncher(testProject, "replicate_created_drafts_and_submissions"));

        String[] launchersToDisable = new String[]{
                "dam_autotag_assets",
                "dm_encode_video_create",
                "dm_encode_video_mod",
                "dm_video_thumbnail_replacement_create",
                "dm_video_thumbnail_replacement_mod",
                "dm_video_user_uploaded_thumbnail_create"
        };

        for (String launcher : launchersToDisable) {
            assertLauncherDisabled(getAbsolutePathForEtcLauncher(testProject, launcher));
        }

        //The legacy project contains only unsupported workflow steps, so this workflow shouldn't be modified
        assertContainsSteps(getAbsolutePathForEtcModel(testProject, "dam/update_asset"), 14);
        assertWorkflowRunnerNotConfigured(testProject);
        assertNoProcessingProfilesCreated(testProject);
        assertNoModulesAddedToReactor(testProject);
        validateLegacyProjectReportOutput();
    }

    @Test(expected = CustomerDataException.class)
    public void testNoWorkflowProjectThrowsCustomerDataException() throws ProjectCreationException, CustomerDataException {
        File testProject  = projectLoader.copyMissingWorkflowProjectToTemp(temp);
        new MigrationOrchestrator(testProject.getPath(), config, reportOutputDir.getPath()).exec();
    }

    private void assertLauncherEnabled(String launcherPath) {
        assertEnabledState(launcherPath, MigrationConstants.TRUE_VALUE);
    }

    private void assertLauncherDisabled(String launcherPath) {
        assertEnabledState(launcherPath, MigrationConstants.FALSE_VALUE);
    }

    private void assertEnabledState(String launcherPath, String state) {
        try {
            File targetLauncher = new File(launcherPath);
            Document targetLauncherXml = XmlUtil.loadXml(targetLauncher);
            String enabled = targetLauncherXml.getFirstChild().getAttributes().getNamedItem(
                    MigrationConstants.ENABLED_PROP).getTextContent();
            assertEquals(state, enabled);
        } catch (Exception e) {
            fail("Exception occurred while evaluating target launcher configuration: " + e.getMessage());
        }
    }

    private String getAbsolutePathForConfLauncher(File tempProjectRoot, String launcherName) {
        String pathRoot = tempProjectRoot.getPath() + File.separator + TestConstants.CONF_WORKFLOW_PROJECT_NAME +
                File.separator + TestConstants.CONF_LAUNCHER_PATH;
        return pathRoot + launcherName + File.separator + MigrationConstants.CONTENT_XML;
    }

    private String getAbsolutePathForEtcLauncher(File tempProjectRoot, String launcherName) {
        String pathRoot = tempProjectRoot.getPath() + File.separator + TestConstants.ETC_WORKFLOW_PROJECT_NAME +
                File.separator + TestConstants.ETC_LAUNCHER_PATH;
        return pathRoot + launcherName + File.separator + MigrationConstants.CONTENT_XML;
    }

    private String getAbsolutePathForConfModel(File tempProjectRoot, String workflowModelName) {
        String pathRoot = tempProjectRoot.getPath() + File.separator + TestConstants.CONF_WORKFLOW_PROJECT_NAME +
                File.separator + TestConstants.CONF_MODEL_PATH;
        return pathRoot + workflowModelName + File.separator + MigrationConstants.CONTENT_XML;
    }

    private String getAbsolutePathForEtcModel(File tempProjectRoot, String workflowModelName) {
        String pathRoot = tempProjectRoot.getPath() + File.separator + TestConstants.ETC_WORKFLOW_PROJECT_NAME +
                File.separator + TestConstants.ETC_MODEL_PATH;
        return pathRoot + workflowModelName + File.separator + MigrationConstants.CONTENT_XML;
    }

    private void assertStepsRemovedFromWorkflowModel(String[] workflowSteps, String workflowModelPath) {
        try {
            File modelFile = new File(workflowModelPath);
            Document modelXml = XmlUtil.loadXml(modelFile);

            List<Node> stepNodes = XmlUtil.getChildElementNodes(modelXml.getElementsByTagName("flow").item(0));

            for (Node stepNode : stepNodes) {
                List<Node> stepChildren = XmlUtil.getChildElementNodes(stepNode);

                for (Node currNode : stepChildren) {
                    if(currNode.getNodeName().equalsIgnoreCase("metadata")) {
                        String processValue = extractProcessValue(currNode);
                        for (String step : workflowSteps) {
                            assertNotEquals("Expected workflow process not removed from the model: " +
                                    workflowModelPath, step, processValue);
                        }
                    }
                }
            }
        } catch (Exception e) {
            fail("Exception occurred while evaluating workflow model configuration: " + e.getMessage());
        }
    }

    private String extractProcessValue(Node metadataNode) {
        String processValue = null;

        Node processNode = metadataNode.getAttributes().getNamedItem("PROCESS");

        if (processNode != null) {
            processValue = processNode.getTextContent();
        } else {
            Node externalProcessNode = metadataNode.getAttributes().getNamedItem("EXTERNAL_PROCESS");

            if (externalProcessNode != null) {
                processValue = externalProcessNode.getTextContent();
            }
        }
        return processValue;
    }

    private void assertContainsSteps(String workflowModelPath, int numSteps) {
        try {
            File modelFile = new File(workflowModelPath);
            Document modelXml = XmlUtil.loadXml(modelFile);

            List<Node> stepNodes = XmlUtil.getChildElementNodes(modelXml.getElementsByTagName("flow").item(0));
            assertEquals(numSteps, stepNodes.size());
        } catch (Exception e) {
            fail("Exception occurred while evaluating workflow model configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void assertWorkflowRunnerConfigExistsForModel(File testProject, String workflowModel) throws ParserConfigurationException, SAXException, IOException {
        File runnerConfig = getRunnerConfig(testProject);
        assertTrue(runnerConfig.exists());
        Document runnerXml = XmlUtil.loadXml(runnerConfig);
        Element rootElem = runnerXml.getDocumentElement();
        List<String> patterns = XmlUtil.getStringArrayListFromAttribute(rootElem, MigrationConstants.WORKFLOW_RUNNER_CONFIG_BY_EXPRESSION);

        boolean matched = false;

        for (String pattern:patterns) {
            String workflow = pattern.split(":")[1];
            if (workflow.contains(workflowModel)) {
                matched = true;
                break;
            }
        }

        assertTrue(matched);
    }

    private void assertWorkflowRunnerNotConfigured(File testProject) {
        File runnerConfig = getRunnerConfig(testProject);
        assertFalse(runnerConfig.exists());
    }

    private File getRunnerConfig(File testProject) {
        return new File(testProject.getPath() + File.separator + MigrationConstants.MIGRATION_PROJECT_APPS +
                File.separator + MigrationConstants.PATH_TO_JCR_ROOT + MigrationConstants.WORKFLOW_RUNNER_CONFIG_PATH +
                File.separator + MigrationConstants.WORKFLOW_RUNNER_CONFIG_FILENAME);
    }

    private void assertProcessingProfilesCreated(File testProject, List<String> profileNames) {
        File profileRoot = new File(testProject.getPath() + File.separator + MigrationConstants.MIGRATION_PROJECT_CONTENT +
                File.separator + MigrationConstants.PROCESSING_PROFILE_DISK_PATH);
        File[] children = profileRoot.listFiles();

        for (String profileName:profileNames) {
            boolean matched = false;

            if (children != null) {
                for (File child:children) {
                    if (child.getName().equals(profileName)) {
                        matched = true;
                    }
                }
            }

            assertTrue(matched);
        }
    }

    private void assertNoProcessingProfilesCreated(File testProject) {
        File profileRoot = new File(testProject.getPath() + File.separator + MigrationConstants.MIGRATION_PROJECT_CONTENT +
                File.separator + MigrationConstants.PROCESSING_PROFILE_DISK_PATH);
        assertFalse(profileRoot.exists());
    }

    private void assertBothNewModulesAddedToReactor(File testProject) throws ParserConfigurationException, SAXException, IOException {
        File reactorPom = new File(testProject, MigrationConstants.POM_XML);
        Document pomDoc = XmlUtil.loadXml(reactorPom);

        assertTrue(hasModule(pomDoc, MigrationConstants.MIGRATION_PROJECT_CONTENT));
        assertTrue(hasModule(pomDoc, MigrationConstants.MIGRATION_PROJECT_APPS));
    }

    private void assertNoModulesAddedToReactor(File testProject) throws ParserConfigurationException, SAXException, IOException {
        File reactorPom = new File(testProject, MigrationConstants.POM_XML);
        Document pomDoc = XmlUtil.loadXml(reactorPom);

        assertFalse(hasModule(pomDoc, MigrationConstants.MIGRATION_PROJECT_CONTENT));
        assertFalse(hasModule(pomDoc, MigrationConstants.MIGRATION_PROJECT_APPS));
    }

    private boolean hasModule(Document pom, String module) {
        NodeList moduleNodes = pom.getElementsByTagName("module");

        for (int i  = 0; i < moduleNodes.getLength(); i++) {
            Node moduleNode = moduleNodes.item(i);
            if (module.equals(moduleNode.getTextContent())) {
                return true;
            }
        }

        return false;
    }

    private void validateConfProjectReportOutput() throws FileNotFoundException {
        File migrationReport = new File(reportOutputDir, MigrationConstants.REPORT_FILENAME);

        Scanner scanner = new Scanner(migrationReport);

        while (scanner.hasNext()) {
            String line = scanner.nextLine();

            if (line.equals("## Workflow Launchers")) {
                validateNumLaunchers(scanner, 8);
            }

            if (line.equals("## Custom Workflow Runner configuration")) {
                skipLines(scanner, 2);
                validateNumberOfTableRows(scanner, 4);
            }

            if (line.equals("## Workflow Model Updates")) {
                skipLines(scanner, 4);

                assertTrue(scanner.nextLine().startsWith("###")); //Workflow model description
                skipLines(scanner, 1);
                validateNumberOfTableRows(scanner, 8);

                assertTrue(scanner.nextLine().startsWith("###")); //Workflow model description
                skipLines(scanner, 1);
                validateNumberOfTableRows(scanner, 8);
            }

            if (line.equals("## Asset Compute Service Processing Profiles")) {
                validateNumProfiles(scanner, 2);
            }

            if (line.equals("## Maven Projects Added and Modified")) {
                validateNumProjects(scanner, 2);
            }
        }
    }

    private void validateLegacyProjectReportOutput() throws FileNotFoundException {
        File migrationReport = new File(reportOutputDir, MigrationConstants.REPORT_FILENAME);

        Scanner scanner = new Scanner(migrationReport);

        while (scanner.hasNext()) {
            String line = scanner.nextLine();

            if (line.equals("## Workflow Launchers")) {
                validateNumLaunchers(scanner, 14);
            }

            if (line.equals("## Custom Workflow Runner configuration")) {
                skipLines(scanner, 2);
                assertEquals(MigrationConstants.NO_RUNNER_CFG_MSG, scanner.nextLine());
            }

            if (line.equals("## Workflow Model Updates")) {
                skipLines(scanner, 4);
                assertEquals(MigrationConstants.NO_MODEL_UPDATE_MSG, scanner.nextLine());
            }

            if (line.equals("## Asset Compute Service Processing Profiles")) {
                skipLines(scanner, 2);
                assertEquals(MigrationConstants.NO_PROFILE_MSG, scanner.nextLine());
            }

            if (line.equals("## Maven Projects Added and Modified")) {
                skipLines(scanner, 2);
                assertEquals(MigrationConstants.NO_PROJECT_MSG, scanner.nextLine());
            }
        }
    }

    private void validateNumLaunchers(Scanner scanner, int numLaunchers) {
        skipLines(scanner, 4);
        validateNumberOfTableRows(scanner, numLaunchers);
    }

    private void validateNumProfiles(Scanner scanner, int numProfiles) {
        skipLines(scanner, 4);
        validateNumberOfTableRows(scanner, numProfiles);
    }

    private void validateNumProjects(Scanner scanner, int numProjects) {
        skipLines(scanner, 4);
        validateNumberOfTableRows(scanner, numProjects);
    }

    private void skipLines(Scanner scanner, int numLines) {
        for (int i = 0; i < numLines; i++) {
            scanner.nextLine();
        }
    }

    private void validateNumberOfTableRows(Scanner scanner, int numRows) {
        for (int i = 0; i < numRows; i++) {
            assertTrue(scanner.nextLine().startsWith("|"));
        }
        assertFalse(scanner.nextLine().startsWith("|"));
    }
}