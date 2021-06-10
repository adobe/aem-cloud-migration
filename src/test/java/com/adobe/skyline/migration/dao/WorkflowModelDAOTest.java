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
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.adobe.skyline.migration.MigrationConstants;
import com.adobe.skyline.migration.SkylineMigrationBaseTest;
import com.adobe.skyline.migration.exception.CustomerDataException;
import com.adobe.skyline.migration.model.workflow.WorkflowModel;
import com.adobe.skyline.migration.model.workflow.WorkflowStep;
import com.adobe.skyline.migration.testutils.TestConstants;
import com.adobe.skyline.migration.util.XmlUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class WorkflowModelDAOTest extends SkylineMigrationBaseTest {

    private static final String PATH_TO_CONF_UPDATE_ASSET = "/conf/global/settings/workflow/models/dam/update_asset";
    private static final String PATH_TO_VAR_UPDATE_ASSET = "/var/workflow/models/dam/update_asset";
    private static final String PATH_TO_ETC_UPDATE_ASSET = "/etc/workflow/models/dam/update_asset";

    private static final String PATH_TO_CONF_ASSET_NOCOMPLETE = "/conf/global/settings/workflow/models/asset_nocomplete";
    private static final String PATH_TO_VAR_ASSET_NOCOMPLETE = "/var/workflow/models/asset_nocomplete";
    private static final String PATH_TO_ETC_ASSET_NOCOMPLETE = "/etc/workflow/models/asset_nocomplete";

    private static final String STEP_GATEKEEPER_PROCESS = "com.day.cq.dam.core.process.GateKeeperProcess";

    private static final WorkflowStep WORKFLOW_COMPLETED_PROCESS_STEP = MigrationConstants.WORKFLOW_COMPLETED_PROCESS;

    private WorkflowModelDAO modelDAO;

    @Before
    public void setUp() {
        super.setUp();

        this.modelDAO = new WorkflowModelDAO();
    }

    /**
     * It is possible for us to run into scenarios where a customer defines a custom launcher for an OOTB workflow model.
     * In these scenarios, we will end up trying to load a workflow model that doesn't exist in the customer's source code.
     * Null should be returned.
     */
    @Test
    public void testMissingConfNode() throws CustomerDataException {
        File projectRoot = projectLoader.copyNoModelProjectToTemp(temp);

        WorkflowModel model = modelDAO.loadWorkflowModel(projectRoot + "/" +
                TestConstants.CONF_WORKFLOW_PROJECT_NAME, "/var/workflow/models/dam-parse-word-documents");

        assertNull(model);
    }

    /**
     * It is possible for customers to check in workflow models that include the nodes under /conf, but not the nodes
     * under /var.  In these cases, we should not set a runtime file but should still include the path to it as we may
     * want to include this path in some configurations.
     */
    @Test
    public void testMissingVarNode() throws CustomerDataException {
        File projectRoot = projectLoader.copyNoModelProjectToTemp(temp);

        WorkflowModel model = modelDAO.loadWorkflowModel(Paths.get(projectRoot.getPath(),
                TestConstants.CONF_WORKFLOW_PROJECT_NAME).toString(),  "/conf/global/settings/workflow/models/dam/update_asset");

        assertNotNull(model.getRuntimeComponent());
        assertNotNull(model.getConfigurationPage());
        assertTrue(model.getConfigurationFile().exists());
    }

    @Test
    public void testModelStepCreatedWithMetadata() throws CustomerDataException {
        File projectRoot = projectLoader.copyConfProjectToTemp(temp);

        WorkflowModel model = modelDAO.loadWorkflowModel(Paths.get(projectRoot.getPath(),
                TestConstants.CONF_WORKFLOW_PROJECT_NAME).toString(), PATH_TO_CONF_UPDATE_ASSET);

        boolean metadataFound = false;

        for (WorkflowStep step : model.getWorkflowSteps()) {
            if ("com.day.cq.dam.core.process.CreatePdfPreviewProcess".equals(step.getProcess())) {
                Map<String, String> metadata = step.getMetadata();

                if (metadata != null) {
                    metadataFound = true;
                    assertEquals("2048", metadata.get("MAX_HEIGHT"));
                    assertEquals("2048", metadata.get("MAX_WIDTH"));
                    assertEquals("[application/pdf,application/postscript,application/illustrator]", metadata.get("MIME_TYPES"));
                    assertEquals("72", metadata.get("RESOLUTION"));

                }
            }
        }

        assertTrue(metadataFound);
    }

    @Test
    public void testStepRemovedFromNewConfFile() throws CustomerDataException {
        File projectRoot = projectLoader.copyConfProjectToTemp(temp);
        String projectPath = projectRoot + "/" + TestConstants.CONF_WORKFLOW_PROJECT_NAME;

        //Load model and ensure it contains the step
        WorkflowModel modelBefore = modelDAO.loadWorkflowModel(projectPath, PATH_TO_CONF_UPDATE_ASSET);
        assertTrue(modelContainsStep(modelBefore, STEP_GATEKEEPER_PROCESS));

        //Remove the step
        modelDAO.removeWorkflowStepFromModel(STEP_GATEKEEPER_PROCESS, modelBefore);

        //Reload the model and ensure the step is gone
        WorkflowModel modelAfter = modelDAO.loadWorkflowModel(projectPath, PATH_TO_CONF_UPDATE_ASSET);
        assertFalse(modelContainsStep(modelAfter, STEP_GATEKEEPER_PROCESS));
    }

    @Test
    public void testStepRemovedFromOldConfFile() throws CustomerDataException {
        File projectRoot = projectLoader.copyEtcProjectToTemp(temp);
        String projectPath = projectRoot + "/" + TestConstants.ETC_WORKFLOW_PROJECT_NAME;

        //Load model and ensure it contains the step
        WorkflowModel modelBefore = modelDAO.loadWorkflowModel(projectPath, PATH_TO_ETC_UPDATE_ASSET);
        assertTrue(modelContainsStep(modelBefore, STEP_GATEKEEPER_PROCESS));

        //Remove the step
        modelDAO.removeWorkflowStepFromModel(STEP_GATEKEEPER_PROCESS, modelBefore);

        //Reload the model and ensure the step is gone
        WorkflowModel modelAfter = modelDAO.loadWorkflowModel(projectPath, PATH_TO_ETC_UPDATE_ASSET);
        assertFalse(modelContainsStep(modelAfter, STEP_GATEKEEPER_PROCESS));
    }

    @Test
    public void testStepAddedToNewConfFile() throws CustomerDataException {
        File projectRoot = projectLoader.copyConfProjectToTemp(temp);
        String projectPath = Paths.get(projectRoot.getPath(), TestConstants.CONF_WORKFLOW_PROJECT_NAME).toString();

        //Load model and ensure it doesn't contain the step
        WorkflowModel modelBefore = modelDAO.loadWorkflowModel(projectPath, PATH_TO_CONF_ASSET_NOCOMPLETE);
        assertFalse("Model already contains the Completed Process.", modelContainsStep(modelBefore,
                WORKFLOW_COMPLETED_PROCESS_STEP.getProcess()));

        //Add the step
        modelDAO.addWorkflowStepToModel(WORKFLOW_COMPLETED_PROCESS_STEP, modelBefore);

        //Reload the model and ensure the step is added
        WorkflowModel modelAfter = modelDAO.loadWorkflowModel(projectPath, PATH_TO_CONF_ASSET_NOCOMPLETE);
        assertTrue("Step was not added to the workflow model.", modelContainsStep(modelAfter,
                WORKFLOW_COMPLETED_PROCESS_STEP.getProcess()));
    }

    @Test
    public void testStepAddedToOldConfFile() throws CustomerDataException {
        File projectRoot = projectLoader.copyEtcProjectToTemp(temp);
        String projectPath = Paths.get(projectRoot.getPath(), TestConstants.ETC_WORKFLOW_PROJECT_NAME).toString();

        //Load model and ensure it doesn't contain the step
        WorkflowModel modelBefore = modelDAO.loadWorkflowModel(projectPath, PATH_TO_ETC_ASSET_NOCOMPLETE);
        assertFalse("Model already contains the Completed Process.", modelContainsStep(modelBefore,
                WORKFLOW_COMPLETED_PROCESS_STEP.getProcess()));

        //Add the step
        modelDAO.addWorkflowStepToModel(WORKFLOW_COMPLETED_PROCESS_STEP, modelBefore);

        //Reload the model and ensure the step is added
        WorkflowModel modelAfter = modelDAO.loadWorkflowModel(projectPath, PATH_TO_ETC_ASSET_NOCOMPLETE);
        assertTrue("Step was not added to the workflow model.", modelContainsStep(modelAfter,
                WORKFLOW_COMPLETED_PROCESS_STEP.getProcess()));
    }

    @Test
    public void testTMG() throws CustomerDataException {
        WorkflowModel model = modelDAO.loadWorkflowModel("/Users/ireasor/Desktop/tmg/customer-test/ui.conf.wf", "/conf/global/settings/workflow/models/dam/update_asset");
    }

    private boolean modelContainsStep(WorkflowModel model, String processId) {
        for (WorkflowStep step : model.getWorkflowSteps()) {
            if (processId.equals(step.getProcess())) {
                return true;
            }
        }
        return false;
    }

    private void assertVarNodes(int numNodes, Document varXml) {
        List<Node> nodes = XmlUtil.getChildElementNodes(varXml.getElementsByTagName(MigrationConstants.NODES_NODE).item(0));
        assertEquals(numNodes, nodes.size());

        Node lastNode = nodes.get(numNodes-1);
        String lastNodeType = lastNode.getAttributes().getNamedItem("type").getTextContent();
        assertEquals("END", lastNodeType);
    }

    private void assertVarTransitions(int numNodes, Document varXml) {
        List<Node> nodes = XmlUtil.getChildElementNodes(varXml.getElementsByTagName(MigrationConstants.TRANSITIONS_NODE).item(0));
        assertEquals(numNodes, nodes.size());

        Node lastNode = nodes.get(numNodes-1);

        String expectedFrom = "node" + (numNodes-1);
        String from = lastNode.getAttributes().getNamedItem("from").getTextContent();
        assertEquals(expectedFrom, from);

        String expectedTo = "node" + (numNodes);
        String to = lastNode.getAttributes().getNamedItem("to").getTextContent();
        assertEquals(expectedTo, to);
    }
}