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

package com.adobe.skyline.migration.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ChangeTrackingService {

    private List<String> disabledLaunchers;
    private Map<String, String> runnerConfigsAdded;
    private Map<String, Map<String, WorkflowStepSupportStatus>> modelStepsModified;
    private List<ProcessingProfile> processingProfilesCreated;
    private List<String> projectsCreated;
    private List<String> varPathsDeleted;
    private Map<String, String> failedMappings;

    public ChangeTrackingService() {
        this.disabledLaunchers = new ArrayList<>();
        this.runnerConfigsAdded = new HashMap<>();
        this.modelStepsModified = new HashMap<>();
        this.processingProfilesCreated = new ArrayList<>();
        this.projectsCreated = new ArrayList<>();
        this.varPathsDeleted = new ArrayList<>();
        this.failedMappings = new HashMap<>();
    }

    public void trackLauncherDisabled(String launcherName) {
        disabledLaunchers.add(launcherName);
    }

    public void trackFailedMappings(String mapping, String failure) {
        this.failedMappings.put(mapping, failure);
    }

    public Map<String, String> getFailedMappings() {
        return failedMappings;
    }

    public List<String> getDisabledLaunchers() {
        return disabledLaunchers;
    }

    public void trackWorkflowRunnerConfigAdded(String glob, String workflowModel) {
        runnerConfigsAdded.put(glob, workflowModel);
    }

    public Map<String, String> getWorkflowRunnerConfigsAdded() {
        return runnerConfigsAdded;
    }

    public void trackModifiedWorkflowStep(String modelPath, String workflowStep, WorkflowStepSupportStatus reason) {
        Map<String, WorkflowStepSupportStatus> valueMap;

        if (modelStepsModified.get(modelPath) != null) {
            valueMap = modelStepsModified.get(modelPath);
        } else {
            valueMap = new LinkedHashMap<>();
            modelStepsModified.put(modelPath, valueMap);
        }

        valueMap.put(workflowStep, reason);
    }

    public Map<String, Map<String, WorkflowStepSupportStatus>> getModifiedWorkflowSteps() {
        return modelStepsModified;
    }

    public void trackCreatedProcessingProfile(ProcessingProfile profile) {
        this.processingProfilesCreated.add(profile);
    }

    public List<ProcessingProfile> getProcessingProfilesCreated() {
        return processingProfilesCreated;
    }

    public void trackProjectCreated(String projectName) {
        projectsCreated.add(projectName);
    }

    public List<String> getProjectsCreated() {
        return projectsCreated;
    }

    public void trackVarPathDeleted(String path) {
        varPathsDeleted.add(path);
    }

    public List<String> getVarPathsDeleted() {
        return varPathsDeleted;
    }
}
