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
import com.adobe.skyline.migration.dao.WorkflowModelDAO;
import com.adobe.skyline.migration.exception.CustomerDataException;
import com.adobe.skyline.migration.model.ChangeTrackingService;
import com.adobe.skyline.migration.model.WorkflowStepSupportStatus;
import com.adobe.skyline.migration.model.workflow.Workflow;
import com.adobe.skyline.migration.model.workflow.WorkflowModel;
import com.adobe.skyline.migration.model.workflow.WorkflowStep;
import com.adobe.skyline.migration.main.WorkflowStepConfiguration;

/**
 * The ModelTransformer first determines if a workflow can run on Skyline environments.  If it can, it will then inspect
 * the workflow and disable any steps that should not be run on Skyline.
 */
public class ModelTransformer {

    private WorkflowStepConfiguration config;
    private WorkflowModelDAO dao;
    private ChangeTrackingService changeTracker;

    public ModelTransformer(WorkflowStepConfiguration config, WorkflowModelDAO dao, ChangeTrackingService changeTracker) {
        this.config = config;
        this.dao = dao;
        this.changeTracker = changeTracker;
    }

    public void transformModel(Workflow workflow) throws CustomerDataException {
        boolean hasWorkflowCompletedProcess = false;

        for (WorkflowStep step : workflow.getWorkflowModel().getWorkflowSteps()) {
            //We require this step in a customer workflow, but shouldn't run a customer workflow just for this step.
            if (step.getProcess().equals(MigrationConstants.WORKFLOW_COMPLETED_PROCESS.getProcess())) {
                hasWorkflowCompletedProcess = true;
            } else if (config.isStepEnabledOnSkyline(step.getProcess()) && !config.isOptionalStep(step.getProcess())) {
                workflow.setCanRunOnSkyline(true);
            }
        }

        /*
         * If the workflow doesn't have any Skyline-enabled steps, there is no need to edit the workflow, we will just
         * disable its launcher.
         */
        if (workflow.getCanRunOnSkyline()) {
            disableInvalidSteps(workflow);

            if (!hasWorkflowCompletedProcess) {
                addRequiredSteps(workflow);
            }
        }
    }

    private void disableInvalidSteps(Workflow workflow) throws CustomerDataException {
        WorkflowModel model = workflow.getWorkflowModel();
        for (WorkflowStep step : model.getWorkflowSteps()) {
            String stepProcess = step.getProcess();
            if(!config.isStepEnabledOnSkyline(stepProcess)) {
                changeTracker.trackModifiedWorkflowStep(model.getConfigurationPage(), stepProcess, config.getStepSupportedStatus(stepProcess));
                dao.removeWorkflowStepFromModel(stepProcess, model);
            }
        }
    }

    private void addRequiredSteps(Workflow workflow) throws CustomerDataException {
        WorkflowModel model = workflow.getWorkflowModel();
        changeTracker.trackModifiedWorkflowStep(model.getConfigurationPage(),
                MigrationConstants.WORKFLOW_COMPLETED_PROCESS.getProcess(), WorkflowStepSupportStatus.REQUIRED);
        dao.addWorkflowStepToModel(MigrationConstants.WORKFLOW_COMPLETED_PROCESS, model);
    }
}
