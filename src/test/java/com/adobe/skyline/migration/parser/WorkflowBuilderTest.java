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

package com.adobe.skyline.migration.parser;

import com.adobe.skyline.migration.SkylineMigrationBaseTest;
import com.adobe.skyline.migration.dao.WorkflowLauncherDAO;
import com.adobe.skyline.migration.dao.WorkflowModelDAO;
import com.adobe.skyline.migration.exception.CustomerDataException;
import com.adobe.skyline.migration.model.workflow.Workflow;
import com.adobe.skyline.migration.model.workflow.WorkflowLauncher;
import com.adobe.skyline.migration.model.workflow.WorkflowModel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class WorkflowBuilderTest extends SkylineMigrationBaseTest {

    private static final String TEST_ROOT = "testRoot";

    @Mock
    private WorkflowLauncherDAO launcherDAO;

    @Mock
    private WorkflowModelDAO modelDAO;

    private WorkflowBuilder builder;

    @Before
    public void setUp() {
        super.setUp();

        this.builder = new WorkflowBuilder(launcherDAO, modelDAO, TEST_ROOT);
    }

    @Test
    public void testOotbModelProcessedWithoutLauncher() throws CustomerDataException {
        String testModelPath = "/var/workflow/models/dam/update_asset";

        List<String> workflowLauncherPaths = new ArrayList<>();
        List<String> workflowModelPaths = new ArrayList<>();
        workflowModelPaths.add(testModelPath);

        WorkflowModel mockModel = new WorkflowModel();
        when(modelDAO.loadWorkflowModel(TEST_ROOT, testModelPath)).thenReturn(mockModel);

        List<Workflow> workflows = builder.buildWorkflows(workflowLauncherPaths, workflowModelPaths);

        assertEquals(1, workflows.size());
        assertEquals(mockModel, workflows.get(0).getWorkflowModel());
    }

    @Test
    public void testOotbModelProcessedForEitherPath() throws CustomerDataException {
        String testModelPath = "/conf/global/settings/workflow/models/dam/update_asset";

        List<String> workflowLauncherPaths = new ArrayList<>();
        List<String> workflowModelPaths = new ArrayList<>();
        workflowModelPaths.add(testModelPath);

        WorkflowModel mockModel = new WorkflowModel();
        when(modelDAO.loadWorkflowModel(TEST_ROOT, testModelPath)).thenReturn(mockModel);

        List<Workflow> workflows = builder.buildWorkflows(workflowLauncherPaths, workflowModelPaths);

        assertEquals(1, workflows.size());
        assertEquals(mockModel, workflows.get(0).getWorkflowModel());
    }

    @Test
    public void testOotbModelNotAddedIfDisabledByLauncher() throws CustomerDataException {
        String testLauncherPath = "/conf/global/settings/workflow/launcher/config/test_launcher";
        String testModelPath = "/var/workflow/models/dam/update_asset";

        List<String> workflowLauncherPaths = new ArrayList<>();
        workflowLauncherPaths.add(testLauncherPath);

        WorkflowLauncher mockLauncher = new WorkflowLauncher();
        mockLauncher.setModelPath(testModelPath);
        mockLauncher.setEnabled(false);
        when(launcherDAO.getWorkflowLauncher(TEST_ROOT, testLauncherPath)).thenReturn(mockLauncher);

        List<String> workflowModelPaths = new ArrayList<>();
        workflowModelPaths.add(testModelPath);

        List<Workflow> workflows = builder.buildWorkflows(workflowLauncherPaths, workflowModelPaths);

        assertEquals(0, workflows.size());
    }

    @Test
    public void testOotbModelNotAddedIfDisabledByLauncherWithOtherPath() throws CustomerDataException {
        String testLauncherPath = "/conf/global/settings/workflow/launcher/config/test_launcher";
        String testModelPath = "/conf/global/settings/workflow/models/dam/update_asset";

        List<String> workflowLauncherPaths = new ArrayList<>();
        workflowLauncherPaths.add(testLauncherPath);

        WorkflowLauncher mockLauncher = new WorkflowLauncher();
        mockLauncher.setModelPath(testModelPath);
        mockLauncher.setEnabled(false);
        when(launcherDAO.getWorkflowLauncher(TEST_ROOT, testLauncherPath)).thenReturn(mockLauncher);

        List<String> workflowModelPaths = new ArrayList<>();
        workflowModelPaths.add(testModelPath);

        List<Workflow> workflows = builder.buildWorkflows(workflowLauncherPaths, workflowModelPaths);

        assertEquals(0, workflows.size());
    }
}