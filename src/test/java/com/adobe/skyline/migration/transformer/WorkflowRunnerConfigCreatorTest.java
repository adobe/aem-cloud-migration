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

package com.adobe.skyline.migration.transformer;

import com.adobe.skyline.migration.MigrationConstants;
import com.adobe.skyline.migration.dao.FilterFileDAO;
import com.adobe.skyline.migration.dao.MavenProjectDAO;
import com.adobe.skyline.migration.exception.ProjectCreationException;
import com.adobe.skyline.migration.model.ChangeTrackingService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.adobe.skyline.migration.SkylineMigrationBaseTest;
import com.adobe.skyline.migration.dao.WorkflowRunnerConfigDAO;
import com.adobe.skyline.migration.model.workflow.Workflow;
import com.adobe.skyline.migration.model.workflow.WorkflowLauncher;
import com.adobe.skyline.migration.model.workflow.WorkflowModel;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class WorkflowRunnerConfigCreatorTest extends SkylineMigrationBaseTest {

    private static final String SAMPLE_VAR_PATH = "/var/workflow/models/dam/update_asset";

    private static final String SAMPLE_GLOB = "/content/dam";
    private static final String SAMPLE_EXPECTED_CONDITION = "/content/dam";

    private static final String SAMPLE_GLOB_2 = "/content/dam(/((?!/subassets)(?!/seasonal).)*/)renditions/original";
    private static final String SAMPLE2_EXPECTED_CONDITION = "/content/dam(/((?!/subassets)(?!/seasonal).)*)";

    private static final String SAMPLE_METADATA_GLOB = "/content/dam(/.*)/jcr:content/metadata";
    private static final String SAMPLE_METADATA_EXPECTED_CONDITION = "/content/dam(.*)";

    @Mock
    private WorkflowRunnerConfigDAO configDAO;

    @Mock
    private FilterFileDAO filterFileDAO;

    @Mock
    private MavenProjectDAO mavenProjectDAO;

    private ChangeTrackingService changeTracker;

    private WorkflowRunnerConfigCreator creator;

    @Before
    public void setUp() {
        super.setUp();

        this.changeTracker = new ChangeTrackingService();

        creator = new WorkflowRunnerConfigCreator(configDAO, filterFileDAO, changeTracker, mavenProjectDAO);
    }

    @Test
    public void testNoRunnableWorkflowsDontCreateConfigsOrUpdateFilterFile() throws ProjectCreationException {
        Workflow workflow = getWorkflow(SAMPLE_VAR_PATH, false);

        creator.createWorkflowConfigs(workflow);

        verify(configDAO, never()).createConfigByExpression(any(), any());
        verify(filterFileDAO, never()).addPath(any());
        verify(mavenProjectDAO, never()).createProject(any());
        assertEquals(0, changeTracker.getWorkflowRunnerConfigsAdded().size());
    }

    @Test
    public void testRunnableWorkflowCreatesConfigAndUpdatesFilterFile() throws ProjectCreationException {
        WorkflowLauncher launcher = getLauncher(SAMPLE_GLOB);
        Workflow workflow = getWorkflow(SAMPLE_VAR_PATH, true);
        workflow.addLauncher(launcher);

        creator.createWorkflowConfigs(workflow);

        verify(configDAO).createConfigByPath(SAMPLE_EXPECTED_CONDITION, SAMPLE_VAR_PATH);
        verify(filterFileDAO).addPath(MigrationConstants.WORKFLOW_RUNNER_CONFIG_PATH);
        verify(mavenProjectDAO).createProject(MigrationConstants.MIGRATION_PROJECT_APPS);
        assertEquals(SAMPLE_VAR_PATH, changeTracker.getWorkflowRunnerConfigsAdded().get(SAMPLE_EXPECTED_CONDITION));
    }

    @Test
    public void testMultipleLaunchersDeDuped() throws ProjectCreationException {
        WorkflowLauncher launcherA = getLauncher(SAMPLE_GLOB);
        WorkflowLauncher launcherB = getLauncher(SAMPLE_GLOB);
        Workflow workflow = getWorkflow(SAMPLE_VAR_PATH, true);
        workflow.addLauncher(launcherA);
        workflow.addLauncher(launcherB);

        creator.createWorkflowConfigs(workflow);

        verify(configDAO, times(1)).createConfigByPath(SAMPLE_EXPECTED_CONDITION, SAMPLE_VAR_PATH);
    }

    @Test
    public void testDifferentLaunchersResultInMultipleConfigs() throws ProjectCreationException {
        WorkflowLauncher launcherA = getLauncher(SAMPLE_GLOB);
        WorkflowLauncher launcherB = getLauncher(SAMPLE_GLOB_2);
        Workflow workflow = getWorkflow(SAMPLE_VAR_PATH, true);
        workflow.addLauncher(launcherA);
        workflow.addLauncher(launcherB);

        creator.createWorkflowConfigs(workflow);

        verify(configDAO, times(1)).createConfigByPath(SAMPLE_EXPECTED_CONDITION, SAMPLE_VAR_PATH);
        verify(configDAO, times(1)).createConfigByExpression(SAMPLE2_EXPECTED_CONDITION, SAMPLE_VAR_PATH);
    }

    @Test
    public void testMetadataTargetedGlobResultsInAssetTargetedConfig() throws ProjectCreationException {
        WorkflowLauncher launcher = getLauncher(SAMPLE_METADATA_GLOB);
        Workflow workflow = getWorkflow(SAMPLE_VAR_PATH, true);
        workflow.addLauncher(launcher);

        creator.createWorkflowConfigs(workflow);

        verify(configDAO).createConfigByExpression(SAMPLE_METADATA_EXPECTED_CONDITION, SAMPLE_VAR_PATH);
    }

    private WorkflowLauncher getLauncher(String glob) {
        WorkflowLauncher launcher = new WorkflowLauncher();
        launcher.setGlob(glob);
        return launcher;
    }

    @SuppressWarnings("SameParameterValue")
    private Workflow getWorkflow(String modelPath, boolean canRunOnSkyline) {
        WorkflowModel model = new WorkflowModel();
        model.setRuntimeComponent(modelPath);

        Workflow workflow = new Workflow();
        workflow.setWorkflowModel(model);
        workflow.setCanRunOnSkyline(canRunOnSkyline);

        return workflow;
    }
}