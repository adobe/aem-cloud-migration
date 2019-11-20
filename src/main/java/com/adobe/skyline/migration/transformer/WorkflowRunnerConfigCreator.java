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

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import com.adobe.skyline.migration.MigrationConstants;
import com.adobe.skyline.migration.dao.FilterFileDAO;
import com.adobe.skyline.migration.dao.MavenProjectDAO;
import com.adobe.skyline.migration.dao.WorkflowRunnerConfigDAO;
import com.adobe.skyline.migration.exception.ProjectCreationException;
import com.adobe.skyline.migration.model.ChangeTrackingService;
import com.adobe.skyline.migration.model.workflow.Workflow;
import com.adobe.skyline.migration.model.workflow.WorkflowLauncher;
import com.adobe.skyline.migration.util.JcrUtil;

/**
 * The WorkflowRunnerConfigCreator generates configurations for the Custom Workflow Runner OSGi service.  On Skyline,
 * this service fills the role that would traditionally be filled by workflow launchers.  Note that we only require
 * configurations if there are workflows to be executed.
 */
public class WorkflowRunnerConfigCreator {

    private WorkflowRunnerConfigDAO configDAO;
    private FilterFileDAO filterFileDAO;
    private ChangeTrackingService changeTracker;
    private MavenProjectDAO mavenProjectDAO;

    public WorkflowRunnerConfigCreator(WorkflowRunnerConfigDAO configDAO, FilterFileDAO filterFileDAO, ChangeTrackingService changeTracker, MavenProjectDAO mavenProjectDAO) {
        this.configDAO = configDAO;
        this.filterFileDAO = filterFileDAO;
        this.changeTracker = changeTracker;
        this.mavenProjectDAO = mavenProjectDAO;
    }

    public void createWorkflowConfigs(Workflow workflow) throws ProjectCreationException {
        boolean configCreated = false;
        boolean projectExists = mavenProjectDAO.projectExists(MigrationConstants.MIGRATION_PROJECT_APPS);

        if (workflow.getCanRunOnSkyline()) {
            String workflowModel = workflow.getWorkflowModel().getRuntimeComponent();

            Set<String> launcherGlobs = new HashSet<>();

            for (WorkflowLauncher launcher : workflow.getLaunchers()) {
                launcherGlobs.add(launcher.getGlob());
            }

            for (String glob : launcherGlobs) {
                //Only create the project if we will actually be writing content to it
                if (!projectExists) {
                    mavenProjectDAO.createProject(MigrationConstants.MIGRATION_PROJECT_APPS);
                    projectExists = true;
                }

                String pattern = glob;

                if (glob.contains("/jcr:content")) {
                    pattern = glob.substring(0, glob.indexOf("/jcr:content"));
                }

                if (JcrUtil.isJcrSafePath(pattern)) {
                    configDAO.createConfigByPath(pattern, workflowModel);
                } else {
                    pattern = convertLauncherGlobToConfigPattern(pattern);
                    configDAO.createConfigByExpression(pattern, workflowModel);
                }

                changeTracker.trackWorkflowRunnerConfigAdded(pattern, workflowModel);
            }

            configCreated = true;
        }

        if (configCreated) {
            filterFileDAO.addPath(MigrationConstants.WORKFLOW_RUNNER_CONFIG_PATH);
        }
    }

    //Launchers target original renditions, but we need to target the Asset node
    private String convertLauncherGlobToConfigPattern(String launcherGlob) {
        //Remove the reference to the original rendition
        String configPattern = launcherGlob.replace(MigrationConstants.ORIGINAL_RENDITION_PATTERN_SUFFIX, "");

        //Remove the last instance of a slash
        int pos = configPattern.lastIndexOf("/");
        if (pos > -1) {
            configPattern = configPattern.substring(0, pos) + configPattern.substring(pos + 1);
        }

        return configPattern;
    }
}
