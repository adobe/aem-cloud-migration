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

import java.util.*;

import com.adobe.skyline.migration.MigrationConstants;
import com.adobe.skyline.migration.dao.WorkflowLauncherDAO;
import com.adobe.skyline.migration.dao.WorkflowModelDAO;
import com.adobe.skyline.migration.exception.CustomerDataException;
import com.adobe.skyline.migration.model.workflow.Workflow;
import com.adobe.skyline.migration.model.workflow.WorkflowLauncher;
import com.adobe.skyline.migration.model.workflow.WorkflowModel;
import com.adobe.skyline.migration.util.Logger;
import org.apache.commons.io.FilenameUtils;

/**
 * Leveraged by the CustomerProjectLoader to create Workflow model objects from the customer workflow configurations.
 */
class WorkflowBuilder {

    private WorkflowModelDAO modelDAO;
    private WorkflowLauncherDAO launcherDAO;
    private String moduleAbsoluteRoot;

    private Set<String> configuredModels;

    WorkflowBuilder(WorkflowLauncherDAO launcherDAO, WorkflowModelDAO modelDAO, String moduleAbsoluteRoot) {
        this.modelDAO = modelDAO;
        this.launcherDAO = launcherDAO;
        this.moduleAbsoluteRoot = moduleAbsoluteRoot;

        configuredModels = new HashSet<>();
    }

    List<Workflow> buildWorkflows(List<String> workflowLauncherPaths, List<String> workflowModelPaths) {
        Map<String, Workflow> workflowsByModel = new HashMap<>();

        populateWorkflowsFromLauncherConfigs(workflowLauncherPaths, workflowsByModel);
        populateWorkflowsFromModelConfigs(workflowModelPaths, workflowsByModel);

        List<Workflow> custWorkflows = new ArrayList<>();
        for (String model : workflowsByModel.keySet()) {
            custWorkflows.add(workflowsByModel.get(model));
        }

        return custWorkflows;
    }

    private void populateWorkflowsFromLauncherConfigs(List<String> workflowLauncherPaths, Map<String, Workflow> workflowsByModel) {
        for (String launcherConfigPath : workflowLauncherPaths) {
            try {
                WorkflowLauncher launcher = launcherDAO.getWorkflowLauncher(moduleAbsoluteRoot, launcherConfigPath);

                configuredModels.add(getModelName(launcher.getModelPath()));
                if (launcher.isEnabled() && isAssetWorkflowLauncher(launcher)) {
                    updateOrAddWorkflowToMap(workflowsByModel, launcher);
                }
            } catch (Exception e) {
                Logger.ERROR("Unable to parse the workflow configuration for " + launcherConfigPath +
                        " or its corresponding workflow model.  This workflow launcher and model will not be migrated.  " +
                        "An exception was caught: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private boolean isAssetWorkflowLauncher(WorkflowLauncher launcher) {
        String glob = launcher.getGlob();
        return glob.contains(MigrationConstants.CONTENT_DAM_PATH) && // In the DAM
                !glob.contains(MigrationConstants.COLLECTIONS) && // Not a collection
                !glob.contains(MigrationConstants.METADATA); // Not a metadata change
    }

    private void updateOrAddWorkflowToMap(Map<String, Workflow> workflowMap, WorkflowLauncher launcher) throws CustomerDataException {
        Workflow workflow;
        String workflowModelPath = launcher.getModelPath();

        if (workflowMap.keySet().contains(workflowModelPath)) {
            workflow = workflowMap.get(workflowModelPath);
        } else {
            workflow = new Workflow();
            WorkflowModel workflowModel = modelDAO.loadWorkflowModel(moduleAbsoluteRoot, workflowModelPath);
            workflow.setWorkflowModel(workflowModel);
            workflowMap.put(workflowModelPath, workflow);
        }

        workflow.addLauncher(launcher);
    }

    /**
     * If OOTB enabled workflows are being overlaid, we need to include them in our migration even if their launchers
     * have not been overlaid
     */
    private void populateWorkflowsFromModelConfigs(List<String> workflowModelPaths, Map<String, Workflow> workflowsByModel) {
        for (String modelPath : workflowModelPaths) {
            String modelName = getModelName(modelPath);
            if (!configuredModels.contains(modelName) &&  MigrationConstants.DEFAULT_ENABLED_MODELS.keySet().contains(modelName)) {
                try {
                    Workflow workflow = new Workflow();
                    WorkflowModel workflowModel = modelDAO.loadWorkflowModel(moduleAbsoluteRoot, modelPath);
                    workflow.setWorkflowModel(workflowModel);
                    workflow.addLauncher(MigrationConstants.DEFAULT_ENABLED_MODELS.get(modelName));
                    workflowsByModel.put(modelPath, workflow);
                } catch (Exception e) {
                    Logger.ERROR("Unable to parse the workflow model for " + modelPath + ".  This model will not " +
                            "be migrated.  An exception was caught: " + e.getMessage());
                    e.printStackTrace();
                }

            }
        }
    }

    private String getModelName(String modelPath) {
        modelPath =  modelPath
                .replaceAll("/jcr:content/model", "")
                .replaceAll("/.content.xml", "");
        return FilenameUtils.getBaseName(modelPath);
    }
}
