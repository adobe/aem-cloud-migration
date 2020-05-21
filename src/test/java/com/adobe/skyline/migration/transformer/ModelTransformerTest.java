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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.adobe.skyline.migration.MigrationConstants;
import com.adobe.skyline.migration.dao.WorkflowModelDAO;
import com.adobe.skyline.migration.exception.CustomerDataException;
import com.adobe.skyline.migration.model.ChangeTrackingService;
import com.adobe.skyline.migration.model.WorkflowStepSupportStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.adobe.skyline.migration.SkylineMigrationBaseTest;
import com.adobe.skyline.migration.model.workflow.Workflow;
import com.adobe.skyline.migration.model.workflow.WorkflowModel;
import com.adobe.skyline.migration.model.workflow.WorkflowStep;

public class ModelTransformerTest extends SkylineMigrationBaseTest {

    /*
     * Properties for workflow model steps that are supported or unsupported are read in from
     * src/main/resources/workflowSteps.properties.
     */
    private static final String UNSUPPORTED_STEP = "com.day.cq.dam.core.process.AssetOffloadingProcess";
    private static final String OPTIONAL_STEP = "com.day.cq.dam.core.impl.process.SendTransientWorkflowCompletedEmailProcess";
    private static final String SUPPORTED_STEP = "com.day.cq.dam.core.impl.process.CreateAssetLanguageCopyProcess";
    private static final WorkflowStep COMPLETED_STEP = MigrationConstants.WORKFLOW_COMPLETED_PROCESS;

    private static final String WORKFLOW_PATH = "/conf/global/settings/workflow/models/dam/update_asset";

    @Mock
    private WorkflowModelDAO mockEditor;

    private ChangeTrackingService changeTracker;

    private ModelTransformer modelTransformer;

    @Before
    public void setUp() {
        super.setUp();

        this.changeTracker = new ChangeTrackingService();

        this.modelTransformer = new ModelTransformer(config, mockEditor, changeTracker);
    }

    @Test
    public void testModelWithNoValidStepsCannotRunOnSkyline() throws CustomerDataException {
        WorkflowStep unsupportedStep = buildStep(UNSUPPORTED_STEP);
        List<WorkflowStep> steps = new ArrayList<>();
        steps.add(unsupportedStep);
        Workflow workflow = buildWorkflow(steps);

        modelTransformer.transformModel(workflow);

        assertFalse(workflow.getCanRunOnSkyline());
        assertEquals(0, changeTracker.getModifiedWorkflowSteps().size());
    }

    @Test
    public void testModelWithNoValidStepsIsUnmodified() throws CustomerDataException {
        WorkflowStep unsupportedStep = buildStep(UNSUPPORTED_STEP);
        List<WorkflowStep> steps = new ArrayList<>();
        steps.add(unsupportedStep);
        Workflow workflow = buildWorkflow(steps);

        modelTransformer.transformModel(workflow);

        verify(mockEditor, never()).removeWorkflowStepFromModel(any(), any());
        assertEquals(0, changeTracker.getModifiedWorkflowSteps().size());
    }

    @Test
    public void testModelWithValidStepsCanRunOnSkyline() throws CustomerDataException {
        WorkflowStep supportedStep = buildStep(SUPPORTED_STEP);
        List<WorkflowStep> steps = new ArrayList<>();
        steps.add(supportedStep);
        Workflow workflow = buildWorkflow(steps);

        modelTransformer.transformModel(workflow);

        assertTrue(workflow.getCanRunOnSkyline());
    }

    @Test
    public void testModelWithValidAndInvalidStepsCanRunOnSkyline() throws CustomerDataException {
        WorkflowStep supportedStep = buildStep(SUPPORTED_STEP);
        WorkflowStep unsupportedStep = buildStep(UNSUPPORTED_STEP);
        List<WorkflowStep> steps = new ArrayList<>();
        steps.add(supportedStep);
        steps.add(unsupportedStep);
        Workflow workflow = buildWorkflow(steps);

        modelTransformer.transformModel(workflow);

        assertTrue(workflow.getCanRunOnSkyline());

        Map<String, Map<String, WorkflowStepSupportStatus>> changedStepMap = changeTracker.getModifiedWorkflowSteps();
        Map<String, WorkflowStepSupportStatus> changedSteps = changedStepMap.get(WORKFLOW_PATH);
        assertEquals(WorkflowStepSupportStatus.NUI_OOTB, changedSteps.get(UNSUPPORTED_STEP));
    }

    @Test
    public void testModelWithValidStepsHasOnlyInvalidStepsRemoved() throws CustomerDataException {
        WorkflowStep supportedStep = buildStep(SUPPORTED_STEP);
        WorkflowStep unsupportedStep = buildStep(UNSUPPORTED_STEP);
        List<WorkflowStep> steps = new ArrayList<>();
        steps.add(supportedStep);
        steps.add(unsupportedStep);
        Workflow workflow = buildWorkflow(steps);

        modelTransformer.transformModel(workflow);

        verify(mockEditor).removeWorkflowStepFromModel(eq(UNSUPPORTED_STEP), eq(workflow.getWorkflowModel()));
        verify(mockEditor, never()).removeWorkflowStepFromModel(eq(OPTIONAL_STEP), eq(workflow.getWorkflowModel()));
        verify(mockEditor, never()).removeWorkflowStepFromModel(eq(SUPPORTED_STEP), eq(workflow.getWorkflowModel()));
    }

    /*
     * The DamUpdateAssetWorkflowCompletedProcess has special cases around it.  We require that it runs at the end of a
     * customer workflow that is executed by the Custom Workflow Runner so that it can mark an asset as Processed.  That
     * being said, we wouldn't want to run it as the _only_ step in a workflow - in cases where a custom workflow is not
     * needed, we are already marking the asset as Processed elsewhere.
     */
    @Test
    public void testModelWithOnlyCompletedProcessAndOptionalStepsAsValidStepIsUnmodified() throws CustomerDataException {
        WorkflowStep unsupportedStep = buildStep(UNSUPPORTED_STEP);
        WorkflowStep optionalStep = buildStep(OPTIONAL_STEP);
        List<WorkflowStep> steps = new ArrayList<>();
        steps.add(unsupportedStep);
        steps.add(optionalStep);
        steps.add(MigrationConstants.WORKFLOW_COMPLETED_PROCESS);
        Workflow workflow = buildWorkflow(steps);

        modelTransformer.transformModel(workflow);

        verify(mockEditor, never()).removeWorkflowStepFromModel(any(), any());
    }

    @Test
    public void testModelWithValidStepsHasCompletedProcessAdded() throws CustomerDataException {
        WorkflowStep supportedStep = buildStep(SUPPORTED_STEP);
        List<WorkflowStep> steps = new ArrayList<>();
        steps.add(supportedStep);
        Workflow workflow = buildWorkflow(steps);

        modelTransformer.transformModel(workflow);

        verify(mockEditor).addWorkflowStepToModel(eq(COMPLETED_STEP), eq(workflow.getWorkflowModel()));

        Map<String, Map<String, WorkflowStepSupportStatus>> changedStepMap = changeTracker.getModifiedWorkflowSteps();
        Map<String, WorkflowStepSupportStatus> changedSteps = changedStepMap.get(WORKFLOW_PATH);
        assertEquals(1, changedSteps.size());
        assertEquals(WorkflowStepSupportStatus.REQUIRED, changedSteps.get(COMPLETED_STEP.getProcess()));
    }

    @Test
    public void testCompletedProcessNotAddedIfAlreadyPresent() throws CustomerDataException {
        WorkflowStep supportedStep = buildStep(SUPPORTED_STEP);
        List<WorkflowStep> steps = new ArrayList<>();
        steps.add(supportedStep);
        steps.add(MigrationConstants.WORKFLOW_COMPLETED_PROCESS);
        Workflow workflow = buildWorkflow(steps);

        modelTransformer.transformModel(workflow);

        verify(mockEditor, never()).addWorkflowStepToModel(eq(COMPLETED_STEP), eq(workflow.getWorkflowModel()));
    }

    private WorkflowStep buildStep(String processId) {
        WorkflowStep step = new WorkflowStep();
        step.setProcess(processId);
        return step;
    }

    private Workflow buildWorkflow(List<WorkflowStep> steps) {
        WorkflowModel model = new WorkflowModel();
        model.setConfigurationPage(WORKFLOW_PATH);
        model.setWorkflowSteps(steps);

        Workflow workflow = new Workflow();
        workflow.setWorkflowModel(model);

        return workflow;
    }
}