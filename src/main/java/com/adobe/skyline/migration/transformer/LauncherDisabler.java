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

import com.adobe.skyline.migration.dao.WorkflowLauncherDAO;
import com.adobe.skyline.migration.model.ChangeTrackingService;
import com.adobe.skyline.migration.model.workflow.Workflow;
import com.adobe.skyline.migration.model.workflow.WorkflowLauncher;
import com.adobe.skyline.migration.model.workflow.WorkflowProject;
import com.adobe.skyline.migration.util.Logger;

/**
 * The LauncherDisabler disables any workflow launchers that are determined to be related to asset processing.  For any
 * workflow models that should continue to be executed, we will configure the Custom DAM Workflow Runner to run those
 * workflows upon Nui processing completion.
 */
public class LauncherDisabler {

    private WorkflowLauncherDAO launcherDAO;
    private ChangeTrackingService changeTracker;

    public LauncherDisabler(WorkflowLauncherDAO launcherDAO, ChangeTrackingService changeTracker) {
        this.launcherDAO = launcherDAO;
        this.changeTracker = changeTracker;
    }

    public void disableLaunchers(WorkflowProject wfProject) {
        for (Workflow workflow : wfProject.getWorkflows()) {
            for (WorkflowLauncher launcher : workflow.getLaunchers()) {
                if (!launcher.isSynthetic()) {
                    disableLauncher(launcher);
                }
            }
        }
    }

    private void disableLauncher(WorkflowLauncher launcher) {
        try {
            launcherDAO.disableLauncher(launcher);
            changeTracker.trackLauncherDisabled(launcher.getName());
        } catch (Exception e) {
            Logger.ERROR("Unable to disable launcher at " + launcher.getRelativePath() +
                    " due to an exception: " + e.getMessage());
        }
    }
}
